package io.smartin.kth.id1212.server.model.User;

import io.smartin.kth.id1212.client.exceptions.ErroneousInputException;
import io.smartin.kth.id1212.server.exceptions.NoSuchTableException;
import io.smartin.kth.id1212.server.exceptions.UserAlreadyExistsException;
import io.smartin.kth.id1212.shared.exceptions.UserNotFoundException;
import io.smartin.kth.id1212.server.integration.DatabaseHandler;
import io.smartin.kth.id1212.shared.DTOs.Credentials;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;

import static io.smartin.kth.id1212.server.integration.DatabaseHandler.Table.USERS;

public class UserManager {
    private final DatabaseHandler db = new DatabaseHandler();
    private final Map<UUID, User> loggedInUsers = Collections.synchronizedMap(new HashMap<>());

    public UserManager () {
        try {
            db.connect("localhost");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUser (UUID userID) throws UserNotFoundException {
        User user = loggedInUsers.get(userID);
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }
        return user;
    }

    public List<User> findUserSessions (String username) {
        List<User> activeSessions = new ArrayList<>();
        synchronized (loggedInUsers) {
            loggedInUsers.forEach((uuid, user) -> {
                if (user.getUsername().equals(username)) {
                    activeSessions.add(user);
                }
            });
        }
        return activeSessions;
    }

    public UUID logIn (User user) {
        UUID userID = UUID.randomUUID();
        loggedInUsers.put(userID, user);
        System.out.println(loggedInUsers.size() + " connected users");
        return userID;
    }

    public User logOut (UUID userID) {
        User user = loggedInUsers.get(userID);
        loggedInUsers.remove(userID);
        return user;
    }

    public void createUser(Credentials credentials) throws UserAlreadyExistsException, SQLException, NoSuchTableException {
        try {
            User user = db.selectUser(credentials.getUsername());
            throw new UserAlreadyExistsException("Username '" + credentials.getUsername() + "' is already taken");
        } catch (UserNotFoundException e) {
            db.insertInto(USERS, credentials.getUsername(), credentials.getPassword());
        }
    }

    public void deleteUser(UUID userID) throws NoSuchTableException, SQLException {
        User user = loggedInUsers.get(userID);
        db.deleteFrom(USERS, user.getUsername());
        List<UUID> toLogOut = new ArrayList<>();
        loggedInUsers.forEach((uuid, activeUser) -> {
            if (activeUser.getUsername().equals(user.getUsername())) {
                toLogOut.add(uuid);
                try {
                    activeUser.forceLogOut();
                    System.out.println("forced out user: " + uuid.toString());
                    activeUser.sendResponse("Logged out because of account deletion");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        toLogOut.forEach(this::logOut);
    }

    public User authenticateUser(Credentials credentials) throws ErroneousInputException, UserNotFoundException, SQLException  {
        User user = (User) db.selectUser(credentials.getUsername());
        if (!passwordsMatch(user, credentials)) {
            throw new ErroneousInputException("Wrong username or password");
        }
        return user;
    }

    private boolean passwordsMatch(User user, Credentials credentials) {
        return user.getPassword().equals(credentials.getPassword());
    }
}
