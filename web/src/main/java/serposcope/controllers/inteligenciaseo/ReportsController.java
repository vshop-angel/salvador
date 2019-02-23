package serposcope.controllers.inteligenciaseo;

import com.google.inject.Inject;
import com.serphacker.serposcope.inteligenciaseo.Report;
import com.serphacker.serposcope.inteligenciaseo.ReportsDB;

import com.serphacker.serposcope.models.base.Group;
import ninja.*;
import ninja.params.Params;

import ninja.session.FlashScope;
import serposcope.controllers.BaseController;
import ninja.params.PathParam;
import serposcope.controllers.google.GoogleGroupController;
import serposcope.filters.AdminFilter;
import serposcope.filters.XSRFFilter;

import java.util.List;

public class ReportsController extends BaseController {
    @Inject
    Router router;

    @Inject
    ReportsDB reportsDB;

    @FilterWith({
            AdminFilter.class,
            XSRFFilter.class
    })

    private Report getReport(Integer reportId) {
        if (reportId == null) {
            return null;
        }
        List<Report> reports = reportsDB.listReports();
        for (Report report : reports) {
            if (report.getId() == reportId){
                return report;
            }
        }
        return null;
    }

    public Result get(@PathParam("reportId") Integer reportId) {
        Report report = getReport(reportId);
        return Results.ok()
                .template("/serposcope/views/ReportsController/report.ftl.html")
                .render("report", report)
        ;
    }
}
