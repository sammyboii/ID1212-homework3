package io.smartin.kth.id1212.server.integration;

import io.smartin.kth.id1212.server.exceptions.NoSuchTableException;
import io.smartin.kth.id1212.shared.exceptions.UserNotFoundException;
import io.smartin.kth.id1212.server.model.File.File;
import io.smartin.kth.id1212.server.model.User.User;
import io.smartin.kth.id1212.shared.DTOs.Metadata;

import java.io.FileNotFoundException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private Connection connection;
    private static final String USERNAME = "root";
    private static final String PASSWORD = "landet92";
    private static final String DATABASE = "id1212hw3";
    private static final String USER_TABLE = "users";
    private static final String FILE_TABLE = "files";

    public DatabaseHandler () {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Could not initialize mariadb Driver");
        }
    }

    private void resetDatabase() throws SQLException {
        String query1 = "DROP TABLE IF EXISTS " + USER_TABLE;
        String query2 = "DROP TABLE IF EXISTS " + FILE_TABLE;
        String query3 = "CREATE TABLE `"+ USER_TABLE +"` (`username` VARCHAR(255), `password` VARCHAR(255) NOT NULL, PRIMARY KEY (`username`))";
        String query4 = "CREATE TABLE "+ FILE_TABLE +" ( `filename` VARCHAR(255), `size` BIGINT NOT NULL, `owner` VARCHAR(255) NOT NULL , `notifications_enabled` BOOLEAN NOT NULL, `is_public` BOOLEAN NOT NULL , `is_readable` BOOLEAN NOT NULL , `is_writable` BOOLEAN NOT NULL , `is_uploaded` BOOLEAN NOT NULL , PRIMARY KEY (`filename`))";
        Statement stmt = connection.createStatement();
        stmt.execute(query1);
        stmt.execute(query2);
        stmt.execute(query3);
        stmt.execute(query4);
    }

    public void connect(String uri) throws SQLException {
        String url = "jdbc:mysql://" + uri + "/" + DATABASE;
        connection = DriverManager.getConnection(url, USERNAME, PASSWORD);
        resetDatabase();
    }

    public void disconnect() throws SQLException {
        connection.close();
    }

    public void insertInto(Table table, String... arguments) throws NoSuchTableException, IllegalArgumentException, SQLException {
        String query;
        switch (table) {
            case USERS:
                if (arguments.length < 2) {
                    throw new IllegalArgumentException("Both username and password must be supplied");
                }
                String username = arguments[0];
                String password = arguments[1];
                query = "INSERT INTO "+ USER_TABLE +" (username, password) VALUES (?, ?)";
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.execute();
                stmt.close();
                break;
            case FILES:
                break;
            default:
                throw new NoSuchTableException("Table interaction not defined");
        }
    }

    public void deleteFrom(Table table, String identifier) throws NoSuchTableException, SQLException {
        String query;
        switch (table) {
            case USERS:
                query = "DELETE FROM users WHERE username=?";
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, identifier);
                stmt.execute();
                stmt.close();
                break;
            case FILES:
                break;
            default:
                throw new NoSuchTableException("Table interaction not defined");
        }
    }

    public User selectUser(String name) throws UserNotFoundException, SQLException {
        String query;
        query = "SELECT * FROM " + USER_TABLE + " WHERE username=?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, name);
        ResultSet rs = stmt.executeQuery();
        String username;
        String password;
        User user;
        if (rs.next()) {
            username = rs.getString("username");
            password = rs.getString("password");
            user = new User(username, password);
            rs.close();
            stmt.close();
            return user;
        }
        rs.close();
        stmt.close();
        throw new UserNotFoundException("User "+ name +" not found");
    }

    public boolean storeFileMetaData (User user, Metadata file) throws SQLException {
        String query = "INSERT INTO `" + FILE_TABLE + "` (`filename`, `size`, `owner`, `notifications_enabled`, `is_public`, `is_readable`, `is_writable`, `is_uploaded`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, file.getName());
        stmt.setLong(2, file.getSize());
        stmt.setString(3, user.getUsername());
        stmt.setBoolean(4, false);
        stmt.setBoolean(5, file.isPublic());
        stmt.setBoolean(6, file.isReadable());
        stmt.setBoolean(7, file.isWritable());
        stmt.setBoolean(8, false);
        stmt.execute();
        stmt.close();
        return true;
    }

    public List<File> getAllFiles() throws SQLException {
        String query = "SELECT * FROM " + FILE_TABLE;
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        List<File> files = new ArrayList<>();
        while (rs.next()) {
            File file = getFileFromResultSet(rs);
            files.add(file);
        }
        rs.close();
        stmt.close();
        return files;
    }

    private File getFileFromResultSet(ResultSet rs) throws SQLException {
        File file = new File(rs.getString("filename"),rs.getLong("size"),
                new User(rs.getString("owner"), null), rs.getBoolean("is_public"),
                rs.getBoolean("is_readable"),rs.getBoolean("is_writable"));
        file.configureNotifications(rs.getBoolean("notifications_enabled"));
        file.setCompleted(rs.getBoolean("is_uploaded"));
        return file;
    }

    public File updateFile(File file) throws SQLException, FileNotFoundException {
        String query = "UPDATE " + FILE_TABLE
                + " SET `size`=?, owner=?, notifications_enabled=?, is_public=?, is_readable=?, " +
                "is_writable=?, is_uploaded=? WHERE filename=?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setLong(1, file.getSize());
        stmt.setString(2, file.getPermissions().getOwner().getUsername());
        stmt.setBoolean(3, file.notifiesOwner());
        stmt.setBoolean(4, file.getPermissions().isPublic());
        stmt.setBoolean(5, file.getPermissions().isReadable());
        stmt.setBoolean(6, file.getPermissions().isWritable());
        stmt.setBoolean(7, file.isCompleted());
        stmt.setString(8,file.getName());
        stmt.execute();
        stmt.close();
        return getFileByName(file.getName());
    }

    public File getFileByName (String filename) throws SQLException, FileNotFoundException {
        String query;
        query = "SELECT * FROM " + FILE_TABLE + " WHERE filename=?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, filename);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            String username = rs.getString("owner");
            File file = getFileFromResultSet(rs);
            rs.close();
            stmt.close();
            return file;
        }
        rs.close();
        stmt.close();
        throw new FileNotFoundException("File "+ filename +" not found");
    }



    public enum Table {
        USERS,
        FILES
    }

    public enum FileProperty {
        FILENAME,
        SIZE,
        OWNER,
        NOTIFICATIONS_ENABLED,
        IS_PUBLIC,
        IS_READABLE,
        IS_WRITABLE,
        IS_UPLOADED
    }
}
