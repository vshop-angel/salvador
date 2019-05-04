package serposcope.controllers.inteligenciaseo;

import com.serphacker.serposcope.inteligenciaseo.Report;
import com.serphacker.serposcope.inteligenciaseo.ReportsDB;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.Router;
import ninja.params.PathParam;
import serposcope.controllers.BaseController;
import serposcope.filters.AdminFilter;
import serposcope.filters.XSRFFilter;

import javax.inject.Inject;

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
        return reportsDB.getReport(reportId);
    }

    public Result get(@PathParam("reportId") Integer reportId) {
        Report report = getReport(reportId);
        return Results.ok()
                .template("/serposcope/views/ReportsController/report.ftl.html")
                .render("report", report)
        ;
    }
}
