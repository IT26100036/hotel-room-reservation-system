USE hotel_db;

INSERT INTO room (room_id, room_no, type, rate, capacity, status) VALUES
(1,  '101', 'SINGLE', 12500.00, 1, 'AVAILABLE'),
(2,  '102', 'SINGLE', 12500.00, 1, 'AVAILABLE'),
(3,  '105', 'DOUBLE', 18000.00, 2, 'AVAILABLE'),
(4,  '107', 'DOUBLE', 18000.00, 2, 'AVAILABLE'),
(5,  '108', 'DOUBLE', 18000.00, 2, 'MAINTENANCE'),
(6,  '202', 'DOUBLE', 18000.00, 2, 'OCCUPIED'),
(7,  '302', 'SUITE',  32000.00, 4, 'AVAILABLE'),
(8,  '306', 'SUITE',  32000.00, 4, 'AVAILABLE'),
(9,  '103', 'SINGLE', 12500.00, 1, 'AVAILABLE'),
(10, '104', 'SINGLE', 12500.00, 1, 'AVAILABLE'),
(11, '106', 'DOUBLE', 18000.00, 2, 'AVAILABLE'),
(12, '201', 'DOUBLE', 18000.00, 2, 'AVAILABLE'),
(13, '203', 'DOUBLE', 18000.00, 2, 'AVAILABLE'),
(14, '301', 'SUITE',  32000.00, 4, 'OCCUPIED'),
(15, '303', 'SUITE',  32000.00, 4, 'AVAILABLE')
ON DUPLICATE KEY UPDATE
    room_no = VALUES(room_no), type = VALUES(type), rate = VALUES(rate),
    capacity = VALUES(capacity), status = VALUES(status);
