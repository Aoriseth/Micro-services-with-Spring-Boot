package com.faros;

import com.faros.model.User;
import com.faros.model.UserRole;
import com.faros.model.UserRoleRepository;
import com.faros.service.UserRoleService;
import com.faros.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javax.servlet.ServletException;
import java.net.URI;
import java.util.Date;
import java.util.List;

/**
 * Created by guang on 2017/5/11.
 */
@RestController
@RequestMapping("/api")
public class APIController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRoleService userRoleService;
    @RequestMapping
    public String info() {
        String info = "Available api under ip:85/api: <br />";
        info += "/addUser (POST: @RequestBody User u) <br />";
        info += "/allUser <br />";
        info += "/allUserRole <br />";
        info += "/saveUserRole (POST: @RequestBody UserRole ur)<br />";
        info += "/usersByRestId?restaurantId={id} <br />";
        info += "/employeesByRestId?restaurantId={id} <br />";
        info +="/deleteUser";
        return info;
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String login(@RequestParam String username, String password) throws ServletException {
        String jwtToken = "";
        if (username == null || password == null) {
            throw new ServletException("Please fill in username and password");
        }
//        String username = login.getUsername();
//        String password = login.getPassword();
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new ServletException("Username not found.");
        }
        String pwd = user.getPassword();
        if (!password.equals(pwd)) {
            throw new ServletException("Invalid login. Please check your name and password.");
        }
        jwtToken = Jwts.builder().setSubject(username).claim("roles", "user").setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, "secretkey").compact();
        return jwtToken;
    }
    /*
    * This api return all Users
    * */
    @RequestMapping(value = "/allUser",method = RequestMethod.GET)
    public List<User> findAllUser() {
        return userService.findAll();
    }
    /*
    * This api return all UserRoles
    * */
    @RequestMapping(value = "/allUserRole",method = RequestMethod.GET)
    public List<UserRole> findAllUserRole() {
        return userRoleService.findAll();
    }

    /*
    * This api do a post to database
    * */
    @RequestMapping(value = "/saveUserRole",method = RequestMethod.POST)
    public ResponseEntity<?> setUserRole(@RequestBody UserRole userRole) {
        //1->user,2->admin,3->manager,4->employee
        //log.info("The UserId is {}, the roleId is {}", userRole.getUserId(),userRole.getRoleId());
        UserRole result = userRoleService.save(userRole);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(result).toUri();
        return ResponseEntity.created(location).build();
    }

    /*
    * This api do a post to database
    * */
    @RequestMapping(value = "/addUser",method = RequestMethod.POST)
    public ResponseEntity<?> addUser(@RequestBody User user) {
        User result = userService.saveUser(user);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(result.getId()).toUri();
        return ResponseEntity.created(location).build();
    }

    /*
    * This api return a list of User by restaurantId
    * */
    @RequestMapping(value = "/usersByRestId",method = RequestMethod.GET)
    public List<User> findUsersByRestaurantId(@RequestParam(value="restaurantId") int restaurantId) {
        return userService.findUsersByRestaurantId(restaurantId);
    }

    /*
    * This api return a list of User by restaurantId
    * */
    @RequestMapping(value = "/employeesByRestId",method = RequestMethod.GET)
    public List<User> findEmployeesByRestaurantId(@RequestParam(value="restaurantId") int restaurantId) {
        return userService.findEmployeesByRestaurantId(restaurantId);
    }

    /*
    * This api return a User by username
    * */
    @RequestMapping(value = "/userByName",method = RequestMethod.GET)
    public User findUsersByRestaurantId(@RequestParam(value="name") String name) {
        return userService.findByUsername(name);
    }

    @DeleteMapping("/deleteUser")
    public void deleteUser(@RequestParam(value = "id") long id) {
        userRoleService.delete(id);
        userService.deleteUser(id);
    }
}
