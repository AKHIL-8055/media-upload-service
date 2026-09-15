CREATE TABLE media (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    s3_file_name VARCHAR(255) NOT NULL
);
