import com.mysql.cj.jdbc.CallableStatement;
import com.mysql.cj.jdbc.MysqlDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Map;
import java.util.stream.Collectors;

public class MusicCallableStatement {

    private static Path basePath = Path.of("PreparedStatement");

    private static final int ARTIST_COLUMN = 0;
    private static final int ALBUM_COLUMN = 1;
    private static final int SONG_COLUMN = 3;

    public static void main(String[] args) {

        Map<String, Map<String, String>> albums = null;

        try (var lines = Files.lines(basePath.resolve("NewAlbums.csv"))) {

            albums = lines.map(s -> s.split(","))
                    .collect(Collectors.groupingBy(s -> s[ARTIST_COLUMN],
                            Collectors.groupingBy(s -> s[ALBUM_COLUMN],
                                    Collectors.mapping(s -> s[SONG_COLUMN],
                                            Collectors.joining(
                                                    "\",\"",
                                                    "[\"",
                                                    "\"]"
                                            )))));

            albums.forEach((artist, artistAlbums) -> {
                System.out.println(artist);
                artistAlbums.forEach((key, value) -> {
                    System.out.println(key + " : " + value);
                });
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var dataSource = new MysqlDataSource();
        dataSource.setServerName("localhost");
        dataSource.setPort(3306);

        try (Connection connection = dataSource.getConnection(
                System.getenv("MYSQL_USER"),
                System.getenv("MYSQL_PASS")
        )) {
            CallableStatement cs = (CallableStatement) connection.prepareCall(
                    "CALL music.addAlbum(?, ?, ?)");

            albums.forEach((artist, albumMap) -> {
                albumMap.forEach((album, songs) -> {
                    try {
                        cs.setString(1, artist);
                        cs.setString(2, album);
                        cs.setString(3, songs);
                        cs.execute();

                    } catch (SQLException e) {
                        System.err.println(e.getErrorCode() + " " + e.getMessage());
                    }
                });
            });

            String sql = "SELECT * FROM music.albumview WHERE artist_name = ?";
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, "Bob Dylan");
            ResultSet resultSet = ps.executeQuery();
            Main.printRecords(resultSet);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
