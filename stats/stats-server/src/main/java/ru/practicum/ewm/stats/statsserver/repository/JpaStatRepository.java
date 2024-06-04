package ru.practicum.ewm.stats.statsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.ewm.stats.statsdto.ViewStats;
import ru.practicum.ewm.stats.statsserver.model.Stat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaStatRepository extends JpaRepository<Stat, Long> {
    @Query("SELECT new ru.practicum.ewm.stats.statsdto.ViewStats( " +
            "st.app, " +
            "st.uri, " +
            "CAST(COUNT(DISTINCT st.ip) AS long)) " +
            "FROM Stat st " +
            "WHERE st.timestamp BETWEEN :start AND :end " +
            "AND st.uri IN :uris " +
            "GROUP BY st.app, st.uri " +
            "ORDER BY COUNT(DISTINCT st.ip) DESC")
    Optional<List<ViewStats>> getStatsByUriAndUniqueIp(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end,
                                                       @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.ewm.stats.statsdto.ViewStats( " +
            "st.app, " +
            "st.uri, " +
            "CAST(COUNT(st.ip) AS long)) " +
            "FROM Stat st " +
            "WHERE st.timestamp BETWEEN :start AND :end " +
            "AND st.uri IN :uris " +
            "GROUP BY st.app, st.uri " +
            "ORDER BY COUNT(st.ip) DESC")
    Optional<List<ViewStats>> getStatsByUriAndNotUniqueIp(@Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end,
                                                          @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.ewm.stats.statsdto.ViewStats( " +
            "st.app, " +
            "st.uri, " +
            "CAST(COUNT(DISTINCT st.ip) AS long)) " +
            "FROM Stat st " +
            "WHERE st.timestamp BETWEEN :start AND :end " +
            "GROUP BY st.app, st.uri " +
            "ORDER BY COUNT(DISTINCT st.ip) DESC")
    Optional<List<ViewStats>> getStatsByWithoutUriAndUniqueIp(@Param("start") LocalDateTime start,
                                                              @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.ewm.stats.statsdto.ViewStats( " +
            "st.app, " +
            "st.uri, " +
            "CAST(COUNT(st.ip) AS long)) " +
            "FROM Stat st " +
            "WHERE st.timestamp BETWEEN :start AND :end " +
            "GROUP BY st.app, st.uri " +
            "ORDER BY COUNT(st.ip) DESC")
    Optional<List<ViewStats>> getStatsByWithoutUriAndNotUniqueIp(@Param("start") LocalDateTime start,
                                                                 @Param("end") LocalDateTime end);
}
