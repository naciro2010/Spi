CREATE TABLE IF NOT EXISTS USUARIOS (
    CodigoUsuario SERIAL PRIMARY KEY,
    Email VARCHAR(255) UNIQUE NOT NULL,
    Password VARCHAR(255),
    Nombre VARCHAR(100),
    Apellidos VARCHAR(100),
    Telefono VARCHAR(20)
);

-- Seed user: password is "1234567" bcrypt-hashed (cost factor 10)
INSERT INTO USUARIOS (Email, Password, Nombre, Apellidos, Telefono)
VALUES ('goldcarweb@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Test', 'User', '+34600000000')
ON CONFLICT (Email) DO NOTHING;
