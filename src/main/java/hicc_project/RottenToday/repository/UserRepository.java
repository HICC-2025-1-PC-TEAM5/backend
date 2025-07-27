package hicc_project.RottenToday.repository;

import hicc_project.RottenToday.entity.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    void update(User user);

    void delete(User user);

    Optional<User> findById(long id);



}
