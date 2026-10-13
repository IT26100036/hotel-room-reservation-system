USE hotel_db;

INSERT INTO booking (booking_id, reservation_id, guest_id, room_id, date_from, date_to, booking_type, status) VALUES
(1, 1,    2,  3,  '2026-10-13', '2026-10-16', 'RESERVATION', 'CONFIRMED'),    -- Anjali, room 105
(2, NULL, 1,  6,  '2026-10-13', '2026-10-15', 'WALK_IN',     'CHECKED_IN'),   -- Ruwan, room 202
(3, 2,    3,  7,  '2026-10-20', '2026-10-22', 'RESERVATION', 'CONFIRMED'),    -- Thomas, room 302
(4, NULL, 4,  9,  '2026-10-13', '2026-10-14', 'WALK_IN',     'CHECKED_OUT'),  -- Kasun, room 103
(5, NULL, 5,  14, '2026-10-13', '2026-10-17', 'WALK_IN',     'CHECKED_IN'),   -- Mei Lin, room 301
(6, NULL, 10, 15, '2026-10-07', '2026-10-09', 'WALK_IN',     'CHECKED_OUT'),  -- Lukas, room 303
(7, NULL, 1,  11, '2026-10-25', '2026-10-27', 'WALK_IN',     'CANCELLED'),    -- Ruwan, room 106
(8, NULL, 9,  10, '2026-10-16', '2026-10-18', 'WALK_IN',     'CONFIRMED')     -- Hana, room 104
ON DUPLICATE KEY UPDATE
    reservation_id = VALUES(reservation_id), guest_id = VALUES(guest_id), room_id = VALUES(room_id),
    date_from = VALUES(date_from), date_to = VALUES(date_to),
    booking_type = VALUES(booking_type), status = VALUES(status);
