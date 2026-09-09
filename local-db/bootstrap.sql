CREATE TABLE IF NOT EXISTS stores (
    storeID INT NOT NULL AUTO_INCREMENT,
    storeName VARCHAR(255) NOT NULL,
    storeHours DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (storeID),
    UNIQUE KEY uq_stores_name (storeName)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS accounts (
    userID INT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(100) NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    nickname VARCHAR(100) NULL,
    role VARCHAR(100) NULL,
    profileBG VARCHAR(32) NOT NULL DEFAULT '#37474F',
    profileText VARCHAR(32) NOT NULL DEFAULT '#FFFFFF',
    inactiveDate DATE NULL,
    PRIMARY KEY (userID),
    UNIQUE KEY uq_accounts_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS employments (
    employmentsID INT NOT NULL AUTO_INCREMENT,
    userID INT NOT NULL,
    storeID INT NOT NULL,
    PRIMARY KEY (employmentsID),
    UNIQUE KEY uq_employments_user_store (userID, storeID),
    KEY ix_employments_store (storeID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS permissions (
    permissionID INT NOT NULL AUTO_INCREMENT,
    permissionName VARCHAR(150) NOT NULL,
    PRIMARY KEY (permissionID),
    UNIQUE KEY uq_permissions_name (permissionName)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS userpermissions (
    userID INT NOT NULL,
    permissionID INT NOT NULL,
    PRIMARY KEY (userID, permissionID),
    KEY ix_userpermissions_permission (permissionID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invoicesuppliers (
    idinvoiceSuppliers INT NOT NULL AUTO_INCREMENT,
    supplierName VARCHAR(255) NOT NULL,
    storeID INT NOT NULL,
    PRIMARY KEY (idinvoiceSuppliers),
    UNIQUE KEY uq_invoice_supplier_store (supplierName, storeID),
    KEY ix_invoice_suppliers_store (storeID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invoices (
    supplierID INT NOT NULL,
    invoiceNo VARCHAR(150) NOT NULL,
    invoiceDate DATE NOT NULL,
    dueDate DATE NULL,
    description TEXT NULL,
    unitAmount DOUBLE NOT NULL DEFAULT 0,
    notes TEXT NULL,
    storeID INT NOT NULL,
    PRIMARY KEY (storeID, supplierID, invoiceNo),
    KEY ix_invoices_month (storeID, invoiceDate),
    KEY ix_invoices_number (invoiceNo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invoicedatapoints (
    storeID INT NOT NULL,
    invoiceNo VARCHAR(150) NOT NULL,
    amount DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (storeID, invoiceNo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS credits (
    idCredits INT NOT NULL AUTO_INCREMENT,
    supplierID INT NOT NULL,
    creditNo VARCHAR(150) NOT NULL,
    referenceInvoiceNo VARCHAR(150) NULL,
    creditDate DATE NOT NULL,
    creditAmount DOUBLE NOT NULL DEFAULT 0,
    notes TEXT NULL,
    storeID INT NOT NULL,
    PRIMARY KEY (idCredits),
    KEY ix_credits_month (storeID, creditDate),
    KEY ix_credits_invoice (referenceInvoiceNo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS accountpaymentcontacts (
    idaccountPaymentContacts INT NOT NULL AUTO_INCREMENT,
    contactName VARCHAR(255) NOT NULL,
    storeID INT NOT NULL,
    accountCode VARCHAR(100) NULL,
    PRIMARY KEY (idaccountPaymentContacts),
    UNIQUE KEY uq_payment_contact_store (contactName, storeID),
    KEY ix_payment_contacts_store (storeID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS accountpayments (
    contactID INT NOT NULL,
    storeID INT NOT NULL,
    invoiceNo VARCHAR(150) NOT NULL,
    invoiceDate DATE NOT NULL,
    dueDate DATE NOT NULL,
    description TEXT NULL,
    unitAmount DOUBLE NOT NULL DEFAULT 0,
    accountAdjusted BOOLEAN NOT NULL DEFAULT FALSE,
    taxRate VARCHAR(50) NULL,
    PRIMARY KEY (storeID, invoiceNo),
    KEY ix_account_payments_month (storeID, invoiceDate),
    KEY ix_account_payments_contact (contactID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tillreportdatapoints (
    storeID INT NOT NULL,
    assignedDate DATE NOT NULL,
    periodStartDate DATE NULL,
    periodEndDate DATE NULL,
    `key` VARCHAR(255) NOT NULL,
    quantity DOUBLE NOT NULL DEFAULT 0,
    amount DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (storeID, assignedDate, `key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS eoddatapoints (
    `date` DATE NOT NULL,
    storeID INT NOT NULL,
    cash DOUBLE NOT NULL DEFAULT 0,
    eftpos DOUBLE NOT NULL DEFAULT 0,
    amex DOUBLE NOT NULL DEFAULT 0,
    googleSquare DOUBLE NOT NULL DEFAULT 0,
    cheque DOUBLE NOT NULL DEFAULT 0,
    medschecks INT NOT NULL DEFAULT 0,
    scriptsOnFile INT NOT NULL DEFAULT 0,
    stockOnHand DOUBLE NOT NULL DEFAULT 0,
    smsPatients INT NOT NULL DEFAULT 0,
    notes TEXT NULL,
    PRIMARY KEY (`date`, storeID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS baschecker (
    `date` DATE NOT NULL,
    storeID INT NOT NULL,
    cashAdjustment DOUBLE NOT NULL DEFAULT 0,
    eftposAdjustment DOUBLE NOT NULL DEFAULT 0,
    amexAdjustment DOUBLE NOT NULL DEFAULT 0,
    googleSquareAdjustment DOUBLE NOT NULL DEFAULT 0,
    chequesAdjustment DOUBLE NOT NULL DEFAULT 0,
    medicareAdjustment DOUBLE NOT NULL DEFAULT 0,
    totalIncomeAdjustment DOUBLE NOT NULL DEFAULT 0,
    cashCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    eftposCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    amexCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    googleSquareCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    chequesCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    medicareCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    totalIncomeCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    gstCorrect BOOLEAN NOT NULL DEFAULT FALSE,
    basDailyScript DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (`date`, storeID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS budgetandexpenses (
    `date` DATE NOT NULL,
    storeID INT NOT NULL,
    monthlyRent DOUBLE NOT NULL DEFAULT 0,
    dailyOutgoings DOUBLE NOT NULL DEFAULT 0,
    buildingOutgoings DOUBLE NOT NULL DEFAULT 0,
    monthlyLoan DOUBLE NOT NULL DEFAULT 0,
    `6CPAIncome` DOUBLE NOT NULL DEFAULT 0,
    LanternPayIncome DOUBLE NOT NULL DEFAULT 0,
    OtherIncome DOUBLE NOT NULL DEFAULT 0,
    ATO_GST_BAS_refund DOUBLE NOT NULL DEFAULT 0,
    monthlyWages DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (`date`, storeID)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS targets (
    `date` DATE NOT NULL,
    storeID INT NOT NULL,
    targetName VARCHAR(255) NOT NULL,
    target1growth DOUBLE NOT NULL DEFAULT 0,
    target1actual DOUBLE NOT NULL DEFAULT 0,
    useTarget1Growth BOOLEAN NOT NULL DEFAULT FALSE,
    target2growth DOUBLE NOT NULL DEFAULT 0,
    target2actual DOUBLE NOT NULL DEFAULT 0,
    useTarget2Growth BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (`date`, storeID, targetName),
    KEY ix_targets_lookup (storeID, targetName, `date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shifts (
    shift_id INT NOT NULL AUTO_INCREMENT,
    storeID INT NOT NULL,
    userID INT NOT NULL,
    shiftStartTime TIME NOT NULL,
    shiftEndTime TIME NOT NULL,
    shiftStartDate DATE NOT NULL,
    shiftEndDate DATE NULL,
    thirtyMinBreaks INT NOT NULL DEFAULT 0,
    tenMinBreaks INT NOT NULL DEFAULT 0,
    repeating BOOLEAN NOT NULL DEFAULT FALSE,
    daysPerRepeat INT NOT NULL DEFAULT 0,
    PRIMARY KEY (shift_id),
    KEY ix_shifts_dates (storeID, shiftStartDate, shiftEndDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shiftmodifications (
    modificationID INT NOT NULL AUTO_INCREMENT,
    storeID INT NOT NULL,
    userID INT NOT NULL,
    shiftStartTime TIME NOT NULL,
    shiftEndTime TIME NOT NULL,
    shiftStartDate DATE NOT NULL,
    shiftEndDate DATE NULL,
    thirtyMinBreaks INT NOT NULL DEFAULT 0,
    tenMinBreaks INT NOT NULL DEFAULT 0,
    repeating BOOLEAN NOT NULL DEFAULT FALSE,
    daysPerRepeat INT NOT NULL DEFAULT 0,
    shift_id INT NOT NULL,
    originalDate DATE NOT NULL,
    PRIMARY KEY (modificationID),
    KEY ix_shift_modifications (shift_id, originalDate),
    KEY ix_shift_modifications_store_date (storeID, shiftStartDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS specialdates (
    eventID INT NOT NULL AUTO_INCREMENT,
    eventDate DATE NOT NULL,
    storeStatus VARCHAR(100) NULL,
    note TEXT NULL,
    PRIMARY KEY (eventID),
    UNIQUE KEY uq_special_dates_date (eventDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS leaverequests (
    leaveID INT NOT NULL AUTO_INCREMENT,
    userID INT NOT NULL,
    storeID INT NOT NULL,
    leaveType VARCHAR(100) NOT NULL,
    leaveStartDate DATETIME NOT NULL,
    leaveEndDate DATETIME NOT NULL,
    reason TEXT NULL,
    PRIMARY KEY (leaveID),
    KEY ix_leave_requests_store_dates (storeID, leaveStartDate, leaveEndDate)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO stores (storeName, storeHours)
VALUES ('Local Test Store', 38)
ON DUPLICATE KEY UPDATE storeHours = VALUES(storeHours);

-- BCrypt hash for the local-only password "admin".
INSERT INTO accounts (
    username, password, first_name, last_name, nickname, role,
    profileBG, profileText, inactiveDate
)
VALUES (
    'admin', '$2a$10$vAPa/SQMfIUu4N2OGrwNL.AzyfUJsM.lKufIyQycDo1a9K7NpyPLu',
    'Local', 'Admin', 'Admin', 'Administrator', '#37474F', '#FFFFFF', NULL
)
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    first_name = VALUES(first_name),
    last_name = VALUES(last_name),
    nickname = VALUES(nickname),
    role = VALUES(role),
    profileBG = VALUES(profileBG),
    profileText = VALUES(profileText),
    inactiveDate = NULL;

INSERT INTO permissions (permissionName) VALUES
    ('EOD - View'),
    ('EOD - Edit'),
    ('Account Payments - View'),
    ('Account Payments - Edit'),
    ('Roster - View'),
    ('Manage Employees - View'),
    ('Users - Edit'),
    ('Stores - Edit'),
    ('Invoicing - View'),
    ('Invoicing - Edit'),
    ('BAS - View'),
    ('BAS - Edit'),
    ('Budget - View'),
    ('Budget - Edit'),
    ('Monthly Summary - View')
ON DUPLICATE KEY UPDATE permissionName = VALUES(permissionName);

INSERT IGNORE INTO employments (userID, storeID)
SELECT accounts.userID, stores.storeID
FROM accounts
CROSS JOIN stores
WHERE accounts.username = 'admin'
  AND stores.storeName = 'Local Test Store';

INSERT IGNORE INTO userpermissions (userID, permissionID)
SELECT accounts.userID, permissions.permissionID
FROM accounts
CROSS JOIN permissions
WHERE accounts.username = 'admin';
