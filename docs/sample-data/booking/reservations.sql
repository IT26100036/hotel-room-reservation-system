USE hotel_db;

INSERT INTO reservation (reservation_id, guest_id, room_id, date_from, date_to, advance_amount, source, status) VALUES
(1, 2, 3,  '2026-10-13', '2026-10-16',  5000.00, 'PHONE', 'CONVERTED'),  -- Anjali, room 105 -> booking 1
(2, 3, 7,  '2026-10-20', '2026-10-22', 20000.00, 'EMAIL', 'CONVERTED'),  -- Thomas, room 302 -> booking 3
(3, 6, 4,  '2026-10-18', '2026-10-20',  5000.00, 'PHONE', 'PENDING'),    -- Dilshan, room 107
(4, 7, 8,  '2026-10-19', '2026-10-22', 15000.00, 'EMAIL', 'PENDING'),    -- Emma, room 306
(5, 8, 1,  '2026-10-14', '2026-10-15',     0.00, 'PHONE', 'PENDING'),    -- Tharindu, room 101, no advance
(6, 9, 12, '2026-10-21', '2026-10-26', 20000.00, 'EMAIL', 'CANCELLED')   -- Hana, room 201
ON DUPLICATE KEY UPDATE
    guest_id = VALUES(guest_id), room_id = VALUES(room_id), date_from = VALUES(date_from),
    date_to = VALUES(date_to), advance_amount = VALUES(advance_amount),
    source = VALUES(source), status = VALUES(status);
