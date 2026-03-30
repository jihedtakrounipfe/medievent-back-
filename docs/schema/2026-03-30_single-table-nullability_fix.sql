ALTER TABLE users MODIFY specialization ENUM('CARDIOLOGY','DERMATOLOGY','GENERAL_PRACTICE','NEUROLOGY','ORTHOPEDICS','OTHER','PEDIATRICS','PSYCHIATRY','RADIOLOGY') NULL;
ALTER TABLE users MODIFY verification_status ENUM('APPROVED','PENDING','REJECTED','SUSPENDED') NULL;
ALTER TABLE users MODIFY consultation_duration INT(11) NULL;
ALTER TABLE users MODIFY consultation_fee DECIMAL(10,3) NULL;
ALTER TABLE users MODIFY is_verified TINYINT(1) NULL DEFAULT 0;

UPDATE users
SET specialization = NULL,
    verification_status = NULL,
    consultation_duration = NULL,
    consultation_fee = NULL
WHERE user_type IN ('PATIENT', 'ADMINISTRATOR');

