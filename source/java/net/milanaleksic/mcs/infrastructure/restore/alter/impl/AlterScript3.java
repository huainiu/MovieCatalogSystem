package net.milanaleksic.mcs.infrastructure.restore.alter.impl;

import net.milanaleksic.mcs.infrastructure.restore.alter.AlterScript;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * User: Milan Aleksic
 * Date: 25/08/11
 * Time: 10:22
 *
 * This Script fills up the denormalized column "MEDIJ_LIST" to allow fast fetching
 * and paging as part of the main movie fetch algorithm
 */
@SuppressWarnings({"HardCodedStringLiteral"})
public class AlterScript3 implements AlterScript {

    protected final Log log = LogFactory.getLog(this.getClass());

    private static String DEFAULT_POZICIJA_NAME = "присутан";

    @Override
    public void executeAlterOnConnection(Connection conn) {
        try {
            conn.setAutoCommit(false);

            try (PreparedStatement st = conn.prepareStatement("SELECT idfilm from DB2ADMIN.Film")) {
                try (ResultSet rs = st.executeQuery()) {
                    Set<Integer> filmIds = new HashSet<>();
                    while (rs.next()) {
                        filmIds.add(rs.getInt(1));
                    }

                    if (log.isInfoEnabled())
                        log.info("Film Ids size: " + filmIds.size());

                    executeProcessing(conn, filmIds);
                }
            }

            conn.commit();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            log.error("SQL Exception occurred while executing alter", e);
        }
    }

    private void executeProcessing(Connection conn, Set<Integer> filmIds) throws SQLException {
        for (Integer idfilm : filmIds) {
            processSingleMovie(conn, idfilm);
        }
    }

    private void processSingleMovie(Connection conn, Integer idfilm) throws SQLException {
        String sql = "SELECT naziv, indeks, pozicija from DB2ADMIN.Zauzima z\n" +
                    "inner join db2admin.medij m on z.idmedij=m.idmedij \n" +
                    "inner join db2admin.tipmedija t on t.idtip=m.idtip \n" +
                    "inner join db2admin.pozicija p on m.idpozicija=p.idpozicija \n" +
                    "where idfilm=?\n" +
                    "order by m.idtip, m.indeks";
        String medijList;
        String filmPosition = DEFAULT_POZICIJA_NAME;

        try (PreparedStatement st = conn.prepareStatement(sql)) {
            st.setInt(1, idfilm);
            try (ResultSet rs = st.executeQuery()) {
                StringBuilder builder = new StringBuilder();
                while (rs.next()) {
                    if (builder.length()!=0)
                        builder.append(" ");
                    String indeks = rs.getString(2);
                    while (indeks.length()<3)
                        indeks = '0'+indeks;
                    builder.append(rs.getString(1)).append(indeks);
                    String position = rs.getString(3);
                    if (!DEFAULT_POZICIJA_NAME.equals(position)) {
                        filmPosition = position;
                    }
                }
                medijList = builder.toString();
                if (log.isInfoEnabled())
                    log.info("Setting medij list: "+medijList+" for movie "+idfilm);
            }
        }

        try (PreparedStatement st = conn.prepareStatement("update DB2ADMIN.Film set MEDIJ_LIST=?, POZICIJA=? where idfilm=?")) {
            st.setString(1, medijList);
            st.setString(2, filmPosition);
            st.setInt(3, idfilm);
            int cntRows = st.executeUpdate();
            if (cntRows != 1)
                log.warn("Could not update movie with id:" + idfilm);
        }
    }

}
