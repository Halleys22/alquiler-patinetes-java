CREATE DATABASE IF NOT EXISTS ALQUILERPATIN CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ALQUILERPATIN;

CREATE TABLE IF NOT EXISTS Usuarios (
    DNI VARCHAR(9) NOT NULL,
    NombreCompleto VARCHAR(100) NOT NULL,
    Email VARCHAR(100) NOT NULL,
    Telefono INT NOT NULL,
    PRIMARY KEY (DNI)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS Patinetes (
    NumSerie INT NOT NULL,
    Color VARCHAR(20) NOT NULL,
    Marca VARCHAR(50) NOT NULL,
    Modelo VARCHAR(50),
    PRIMARY KEY (NumSerie)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS Registro (
    ID INT NOT NULL AUTO_INCREMENT,
    DNI VARCHAR(9) NOT NULL,
    NumSerie INT NOT NULL,
    FechaRecog DATE NOT NULL,
    FechaDev DATE NOT NULL,
    PRIMARY KEY (ID),
    KEY idx_registro_dni_id (DNI, ID),
    CONSTRAINT fk_registro_usuario FOREIGN KEY (DNI) REFERENCES Usuarios (DNI),
    CONSTRAINT fk_registro_patinete FOREIGN KEY (NumSerie) REFERENCES Patinetes (NumSerie)
) ENGINE = InnoDB;

-- Actualiza las fechas de una base creada con el esquema anterior.
SET @migracion_fechas = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND LOWER(TABLE_NAME) = 'registro'
          AND COLUMN_NAME IN ('FechaRecog', 'FechaDev')
          AND DATA_TYPE <> 'date'
    ),
    'ALTER TABLE Registro MODIFY FechaRecog DATE NOT NULL, MODIFY FechaDev DATE NOT NULL',
    'SELECT 1'
);
PREPARE migrar_fechas FROM @migracion_fechas;
EXECUTE migrar_fechas;
DEALLOCATE PREPARE migrar_fechas;

-- Si la base de clase tiene una clave manual, conserva los IDs y genera los siguientes.
SET @migracion_id = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND LOWER(TABLE_NAME) = 'registro'
          AND COLUMN_NAME = 'ID'
          AND EXTRA NOT LIKE '%auto_increment%'
    ),
    'ALTER TABLE Registro MODIFY ID INT NOT NULL AUTO_INCREMENT',
    'SELECT 1'
);
PREPARE migrar_id FROM @migracion_id;
EXECUTE migrar_id;
DEALLOCATE PREPARE migrar_id;

-- Las series existentes conservan sus datos cuando se vuelve a ejecutar el script.
INSERT INTO Patinetes (NumSerie, Color, Marca, Modelo) VALUES
    (1001, 'Negro', 'Xiaomi', 'Electric Scooter 4'),
    (1002, 'Gris', 'Xiaomi', 'Electric Scooter 4 Lite'),
    (1003, 'Negro', 'Xiaomi', 'Electric Scooter 4 Pro'),
    (1004, 'Blanco', 'Xiaomi', 'Electric Scooter 4 Go'),
    (1005, 'Gris', 'Xiaomi', 'Mi Electric Scooter 3'),
    (1006, 'Negro', 'Xiaomi', 'Mi Electric Scooter Pro 2'),
    (1007, 'Azul', 'Xiaomi', 'Electric Scooter 4 Ultra'),
    (1008, 'Gris', 'Segway', 'Ninebot F2'),
    (1009, 'Negro', 'Segway', 'Ninebot F2 Plus'),
    (1010, 'Negro', 'Segway', 'Ninebot F2 Pro'),
    (1011, 'Negro', 'Segway', 'Ninebot Max G30'),
    (1012, 'Gris', 'Segway', 'Ninebot Max G2'),
    (1013, 'Negro', 'Segway', 'Ninebot E2'),
    (1014, 'Azul', 'Segway', 'Ninebot E2 Plus'),
    (1015, 'Negro', 'Segway', 'Ninebot D28E'),
    (1016, 'Rojo', 'Segway', 'Ninebot D38E'),
    (1017, 'Blanco', 'Cecotec', 'Bongo Serie A'),
    (1018, 'Negro', 'Cecotec', 'Bongo Serie A Advance'),
    (1019, 'Gris', 'Cecotec', 'Bongo Serie A Connected'),
    (1020, 'Negro', 'Cecotec', 'Bongo Serie Z'),
    (1021, 'Negro', 'Cecotec', 'Bongo Serie S'),
    (1022, 'Verde', 'Cecotec', 'Bongo Serie X'),
    (1023, 'Negro', 'Cecotec', 'Bongo D20'),
    (1024, 'Gris', 'Cecotec', 'Bongo D30'),
    (1025, 'Negro', 'Cecotec', 'Bongo D40'),
    (1026, 'Rojo', 'Razor', 'E Prime III'),
    (1027, 'Negro', 'Razor', 'E300'),
    (1028, 'Azul', 'Razor', 'Power Core E90'),
    (1029, 'Blanco', 'Razor', 'E Prime Air'),
    (1030, 'Negro', 'NIU', 'KQi2 Pro'),
    (1031, 'Gris', 'NIU', 'KQi3 Sport'),
    (1032, 'Negro', 'NIU', 'KQi3 Pro'),
    (1033, 'Negro', 'NIU', 'KQi3 Max'),
    (1034, 'Blanco', 'NIU', 'KQi1 Sport'),
    (1035, 'Gris', 'SmartGyro', 'Speedway'),
    (1036, 'Negro', 'SmartGyro', 'Speedway Pro'),
    (1037, 'Verde', 'SmartGyro', 'K2'),
    (1038, 'Negro', 'SmartGyro', 'K2 Pro'),
    (1039, 'Rojo', 'SmartGyro', 'Crossover Dual'),
    (1040, 'Azul', 'SmartGyro', 'Ziro'),
    (1041, 'Negro', 'Inmotion', 'Air'),
    (1042, 'Gris', 'Inmotion', 'Air Pro'),
    (1043, 'Negro', 'Inmotion', 'Climber'),
    (1044, 'Negro', 'Pure Electric', 'Air3'),
    (1045, 'Blanco', 'Pure Electric', 'Air3 Pro'),
    (1046, 'Gris', 'Pure Electric', 'Advance'),
    (1047, 'Negro', 'Gotrax', 'GXL V2'),
    (1048, 'Rojo', 'Gotrax', 'G4'),
    (1049, 'Azul', 'Gotrax', 'G5'),
    (1050, 'Negro', 'Gotrax', NULL)
ON DUPLICATE KEY UPDATE NumSerie = NumSerie;
