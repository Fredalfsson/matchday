CREATE TABLE messages (
    id UUID PRIMARY KEY,
    circle_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT messages_content_length_check
        CHECK (char_length(content) BETWEEN 1 AND 1000),
    CONSTRAINT messages_circle_id_fk
        FOREIGN KEY (circle_id) REFERENCES circles (id)
);

CREATE INDEX messages_circle_created_at_id_idx
    ON messages (circle_id, created_at DESC, id DESC);
