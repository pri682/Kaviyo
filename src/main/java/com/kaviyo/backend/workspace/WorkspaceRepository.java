package com.kaviyo.backend.workspace;

import com.kaviyo.backend.catalog.CatalogImportPersistenceException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WorkspaceRepository {
    @Inject DataSource dataSource;

    public List<Workspace> listForSubject(String subject) {
        List<Workspace> workspaces = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT w.id, w.name
                     FROM workspace_memberships m
                     JOIN workspaces w ON w.id = m.workspace_id
                     WHERE m.subject = ?
                     ORDER BY w.name, w.id
                     """)) {
            statement.setString(1, subject);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) workspaces.add(new Workspace(rows.getObject("id", UUID.class), rows.getString("name")));
            }
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("Workspaces could not be loaded.", exception);
        }
        return List.copyOf(workspaces);
    }

    public boolean isMember(String subject, UUID workspaceId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM workspace_memberships WHERE subject = ? AND workspace_id = ?")) {
            statement.setString(1, subject);
            statement.setObject(2, workspaceId);
            try (ResultSet rows = statement.executeQuery()) { return rows.next(); }
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("Workspace access could not be checked.", exception);
        }
    }

    public Workspace createForSubject(String subject, String name) {
        UUID id = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement workspace = connection.prepareStatement(
                        "INSERT INTO workspaces(id, name) VALUES (?, ?)")) {
                    workspace.setObject(1, id);
                    workspace.setString(2, name);
                    workspace.executeUpdate();
                }
                try (PreparedStatement membership = connection.prepareStatement(
                        "INSERT INTO workspace_memberships(workspace_id, subject, role) VALUES (?, ?, 'OWNER')")) {
                    membership.setObject(1, id);
                    membership.setString(2, subject);
                    membership.executeUpdate();
                }
                connection.commit();
                return new Workspace(id, name);
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new CatalogImportPersistenceException("Workspace could not be created.", exception);
        }
    }

    public record Workspace(UUID id, String name) { }
}
