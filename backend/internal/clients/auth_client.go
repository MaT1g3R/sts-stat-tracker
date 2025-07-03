package clients

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

const (
	defaultTimeout     = 5 * time.Second
	defaultIdleTimeout = 30 * time.Second
	maxIdleConnections = 100
)

type AuthClient struct {
	BaseURL string

	httpClient *http.Client
}

func NewAuthClient(baseURL string) *AuthClient {
	transport := &http.Transport{
		MaxIdleConns:       maxIdleConnections,
		IdleConnTimeout:    defaultIdleTimeout,
		DisableCompression: true,
	}

	client := &http.Client{
		Transport: transport,
		Timeout:   defaultTimeout,
	}

	return &AuthClient{
		BaseURL:    baseURL,
		httpClient: client,
	}
}

type authRequest struct {
	ID     string `json:"id"`
	Secret string `json:"secret"`
}

type authResponse struct {
	User string `json:"user"`
}

func (c *AuthClient) Authenticate(ctx context.Context, userID, token string) (_ string, err error) {
	path := "/api/v1/login"

	reqBody := authRequest{
		ID:     userID,
		Secret: token,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return "", fmt.Errorf("failed to marshal request: %w", err)
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.BaseURL+path, bytes.NewBuffer(jsonData))
	if err != nil {
		return "", fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("failed to send request: %w", err)
	}
	defer func() {
		closeErr := resp.Body.Close()
		if closeErr != nil {
			err = fmt.Errorf("request error: %w, close error: %w", err, closeErr)
		}
	}()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("unexpected status code: %d", resp.StatusCode)
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("failed to read response body: %w", err)
	}

	var authResp authResponse
	if err := json.Unmarshal(body, &authResp); err != nil {
		return "", fmt.Errorf("failed to unmarshal response: %w", err)
	}

	return authResp.User, nil
}
