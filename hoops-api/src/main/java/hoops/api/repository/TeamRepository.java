package hoops.api.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class TeamRepository {
    private final DataSource dataSource;

    @Autowired
    public TeamRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void example() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM teams")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                // Process results
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        }
    }
} 