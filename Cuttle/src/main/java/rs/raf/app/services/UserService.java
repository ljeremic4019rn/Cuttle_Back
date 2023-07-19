package rs.raf.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import rs.raf.app.model.User;
import rs.raf.app.repositories.UserRepository;
import rs.raf.app.responses.ResponseDto;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private PasswordEncoder passwordEncoder;

    private UserRepository userRepository;

    @Autowired
    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository, TaskScheduler taskScheduler) {
        this.passwordEncoder = passwordEncoder;

        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> myUser = this.findByUsername(username);
        if(!myUser.isPresent()) {
            throw new UsernameNotFoundException("User name <" + username + "> not found");
        }

        return new org.springframework.security.core.userdetails.User(myUser.get().getUsername(), myUser.get().getPassword(), new ArrayList<>());
    }

    public ResponseDto create(User user) {
        Optional<User> potentialUser = this.findByUsername(user.getUsername());

        if (potentialUser.isPresent()) {
            return new ResponseDto("Username already exists", 409);
        }

        user.setPassword(this.passwordEncoder.encode(user.getPassword()));
        user = this.userRepository.save(user);
        System.out.println("Service: User created");
        return new ResponseDto("User successfully created", 200);
    }

    public Page<User> paginate(Integer page, Integer size) {
        return this.userRepository.findAll(PageRequest.of(page, size, Sort.by("salary").descending()));
    }

    public Optional<User> findByUsername(String username) {
        return this.userRepository.findByUsername(username);
    }
}
