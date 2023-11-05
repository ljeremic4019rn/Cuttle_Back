package rs.raf.app.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.raf.app.model.User;

import java.util.Optional;

@Repository()
public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
