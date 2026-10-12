package com.hotel.reservation.common;

import java.util.List;

// Contract every DAO implements (StaffDao, RoomDao, BookingDao, ...).
// Each member writes their own implementation with JDBC.
// @param <T> the entity type handled by the DAO

public interface CrudDao<T extends BaseEntity> {
    // Inserts a new row and returns the generated id
    int create(T entity);

    // Returns the entity with this id, or null if it does not exist
    T findById(int id);

    // Returns all rows of this table
    List<T> findAll();

    // Saves changes to an existing row. Returns true if a row was updated
    boolean update(T entity);

    // Deletes the row with this id. Returns true if a row was deleted
    boolean delete(int id);
}
