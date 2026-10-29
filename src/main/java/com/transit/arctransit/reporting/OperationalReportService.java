package com.transit.arctransit.reporting;

import com.transit.arctransit.common.validation.SafeText;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface OperationalReportService {

    OperationalReportView submitReport(@NotNull @Valid CreateOperationalReportCommand command);

    OperationalReportView verifyReport(@NotNull @Positive Long id, @NotBlank @Size(max = 50) @SafeText String status, @NotBlank @Size(max = 500) @SafeText(multiline = true) String reason);

    OperationalReportView getReport(@NotNull @Positive Long id);

    Page<OperationalReportView> searchReports(Pageable pageable);

    void archiveReport(@NotNull @Positive Long id);

    void restoreReport(@NotNull @Positive Long id);

    void hardDeleteReport(@NotNull @Positive Long id);

    Page<OperationalReportView> searchArchivedReports(@Size(max = 200) @SafeText String filter, Pageable pageable);

    List<OperationalReportView> getReviewCandidates(Instant staleBefore);

    List<ReportVerificationView> getVerificationHistory(@NotNull @Positive Long id);

    long countUnverifiedReports();

    long countStaleReports();
}
