USE hotel_db;

INSERT INTO guest (guest_id, name, nic_passport, contact, email) VALUES
(1,  'Ruwan Silva',            '199012345678', '0771234567', 'ruwan.s@mail.lk'),
(2,  'Anjali Fernando',        '199545612345', '0774561234', 'anjali.f@mail.lk'),
(3,  'Thomas Becker',          'P4829301',     '0765550123', 'tbecker@mail.de'),
(4,  'Kasun Jayawardena',      '198834567V',   '0701234567', NULL),
(5,  'Mei Lin Zhou',           'E7712345',     '0779990000', 'meilin@mail.cn'),
(6,  'Dilshan Wickramasinghe', '199134567V',   '0771122334', NULL),
(7,  'Emma Clarke',            'P5551234',     '0723334455', 'emma.c@mail.uk'),
(8,  'Tharindu Bandara',       '200012345678', '0786655443', NULL),
(9,  'Hana Sato',              'TK123456',     '0788889999', NULL),
(10, 'Lukas Weber',            'C01X00747',    '0765552211', 'l.weber@mail.de')
ON DUPLICATE KEY UPDATE
    name = VALUES(name), nic_passport = VALUES(nic_passport),
    contact = VALUES(contact), email = VALUES(email);
