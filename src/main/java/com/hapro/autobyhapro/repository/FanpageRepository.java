package com.hapro.autobyhapro.repository;

import com.hapro.autobyhapro.database.DatabaseManager;
import com.hapro.autobyhapro.entity.Fanpage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FanpageRepository {

    public String generateNextPageCode() {
        String sql = "SELECT MAX(id) AS max_id FROM fanpages";

        try (Connection connection = DatabaseManager.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            long nextId = 1;

            if (resultSet.next()) {
                nextId = resultSet.getLong("max_id") + 1;
            }

            return String.format("P%03d", nextId);

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tạo mã fanpage tiếp theo.", exception);
        }
    }

    public void save(Fanpage fanpage) {
        String sql = """
                INSERT INTO fanpages (
                    page_code,
                    page_name,
                    page_url,
                    niche,
                    default_video_count
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, fanpage.getPageCode());
            preparedStatement.setString(2, fanpage.getPageName());
            preparedStatement.setString(3, fanpage.getPageUrl());
            preparedStatement.setString(4, fanpage.getNiche());
            preparedStatement.setInt(5, fanpage.getDefaultVideoCount());

            preparedStatement.executeUpdate();

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lưu fanpage.", exception);
        }
    }

    public List<Fanpage> findAll() {
        String sql = """
                SELECT
                    id,
                    page_code,
                    page_name,
                    page_url,
                    niche,
                    default_video_count,
                    start_date,
                    active,
                    created_time
                FROM fanpages
                ORDER BY id DESC
                """;

        List<Fanpage> fanpages = new ArrayList<>();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                Fanpage fanpage = new Fanpage(
                        resultSet.getLong("id"),
                        resultSet.getString("page_code"),
                        resultSet.getString("page_name"),
                        resultSet.getString("page_url"),
                        resultSet.getString("niche"),
                        resultSet.getInt("default_video_count"),
                        resultSet.getString("start_date"),
                        resultSet.getInt("active") == 1,
                        resultSet.getString("created_time")
                );

                fanpages.add(fanpage);
            }

            return fanpages;

        } catch (SQLException exception) {
            throw new RuntimeException("Không thể lấy danh sách fanpage.", exception);
        }
    }
}
