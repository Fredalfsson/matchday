CREATE TABLE circles (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT circles_match_id_unique UNIQUE (match_id),
    CONSTRAINT circles_match_id_fk FOREIGN KEY (match_id) REFERENCES matches (id)
);

CREATE TABLE circle_memberships (
    circle_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (circle_id, user_id),
    CONSTRAINT circle_memberships_circle_id_fk
        FOREIGN KEY (circle_id) REFERENCES circles (id)
);
