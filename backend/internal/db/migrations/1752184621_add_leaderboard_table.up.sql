CREATE TYPE leaderboard_kind as ENUM ('streak', 'speedrun');

CREATE TABLE leaderboard
(
    username      VARCHAR(50)      NOT NULL REFERENCES users (username) ON DELETE CASCADE,
    character     VARCHAR(50)      NOT NULL REFERENCES characters (name),
    kind          leaderboard_kind NOT NULL,

    date_achieved DATE             NOT NULL,
    score         DOUBLE PRECISION NOT NULL,

    PRIMARY KEY (username, character, kind)
);

CREATE TYPE monthly_leaderboard_kind as ENUM ('winrate-monthly');
CREATE TABLE monthly_leaderboard
(
    username      VARCHAR(50)              NOT NULL REFERENCES users (username) ON DELETE CASCADE,
    character     VARCHAR(50)              NOT NULL REFERENCES characters (name),
    kind          monthly_leaderboard_kind NOT NULL,
    month         DATE                     NOT NULL,

    date_achieved DATE                     NOT NULL,
    score         DOUBLE PRECISION         NOT NULL,

    PRIMARY KEY (username, character, kind, month)
);
