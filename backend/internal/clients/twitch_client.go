package clients

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"sync/atomic"
)

type TwitchClient struct {
	httpClient *http.Client

	clientID     string
	clientSecret string

	accessToken *atomic.Value
}

func NewTwitchClient(clientID, clientSecret string) *TwitchClient {
	transport := &http.Transport{
		MaxIdleConns:       maxIdleConnections,
		IdleConnTimeout:    defaultIdleTimeout,
		DisableCompression: true,
	}

	client := &http.Client{
		Transport: transport,
		Timeout:   defaultTimeout,
	}

	return &TwitchClient{
		httpClient:   client,
		clientID:     clientID,
		clientSecret: clientSecret,
		accessToken:  &atomic.Value{},
	}
}

type getUserData struct {
	ProfileImageUrl string `json:"profile_image_url"`
}
type getUserResponse struct {
	Data []getUserData `json:"data"`
}

type authResp struct {
	AccessToken string `json:"access_token"`
}

func (t *TwitchClient) authenticate(ctx context.Context) (string, error) {
	url := fmt.Sprintf("https://id.twitch.tv/oauth2/token?client_id=%s&client_secret=%s&grant_type=client_credentials", t.clientID, t.clientSecret)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, url, nil)
	if err != nil {
		return "", err
	}
	resp, err := t.httpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		return "", errors.New(resp.Status)
	}
	var authResp authResp
	if err := json.NewDecoder(resp.Body).Decode(&authResp); err != nil {
		return "", err
	}
	return authResp.AccessToken, nil
}

func (t *TwitchClient) doRequest(ctx context.Context,
	method string, url string, body io.Reader, retry bool) (*http.Response, error) {
	if t.accessToken.Load() == nil {
		accessToken, err := t.authenticate(ctx)
		if err != nil {
			return nil, err
		}
		t.accessToken.Store(accessToken)
	}

	req, err := http.NewRequestWithContext(ctx, method, url, body)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", fmt.Sprintf("Bearer %s", t.accessToken.Load().(string)))
	req.Header.Set("Client-ID", t.clientID)
	req.Header.Set("Content-Type", "application/json")
	resp, err := t.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	if resp.StatusCode == http.StatusUnauthorized && !retry {
		defer resp.Body.Close()
		accessToken, err := t.authenticate(ctx)
		if err != nil {
			return nil, err
		}
		t.accessToken.Store(accessToken)
		return t.doRequest(ctx, method, url, body, true)
	}

	if resp.StatusCode != http.StatusOK {
		return nil, errors.New(resp.Status)
	}

	return resp, nil
}

func (t *TwitchClient) GetUserProfileImage(ctx context.Context, id string) (string, error) {
	url := "https://api.twitch.tv/helix/users?id=" + id
	resp, err := t.doRequest(ctx, http.MethodGet, url, nil, false)

	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var response getUserResponse
	err = json.NewDecoder(resp.Body).Decode(&response)
	if err != nil {
		return "", err
	}
	if len(response.Data) == 0 {
		return "", errors.New("not found")
	}
	return response.Data[0].ProfileImageUrl, nil
}
