document.leaderboardFilterAction = (myName, myValue, includedFilterIDs) => {
    const formData = new FormData(document.getElementById("leaderboard-filters"));
    const baseURL = "/app/leaderboards?";
    const params = new URLSearchParams();

    params.append(myName, myValue);


    for (const id of includedFilterIDs) {
        let val = formData.get(id);
        if (!val) {
            continue;
        }
        val = val.trim();
        const name = id.replace("leaderboard-", "").trim();
        if (name.length > 0 && val && val.length > 0) {
            params.append(name, val);
        }
    }

    window.location.href = baseURL + params.toString();
};