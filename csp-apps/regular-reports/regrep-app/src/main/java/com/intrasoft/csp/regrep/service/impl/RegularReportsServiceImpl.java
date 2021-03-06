package com.intrasoft.csp.regrep.service.impl;

import com.intrasoft.csp.regrep.CspDataMappingType;
import com.intrasoft.csp.regrep.DateMath;
import com.intrasoft.csp.regrep.ElasticSearchClient;
import com.intrasoft.csp.regrep.LogstashMappingType;
import com.intrasoft.csp.regrep.commons.model.HitsItem;
import com.intrasoft.csp.regrep.commons.model.Mail;
import com.intrasoft.csp.regrep.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.intrasoft.csp.regrep.DateMath.*;
import static com.intrasoft.csp.regrep.service.Basis.*;


@Service
public class RegularReportsServiceImpl implements RegularReportsService {

    private static final Logger LOG = LoggerFactory.getLogger(RegularReportsServiceImpl.class);

    @Autowired
    RegularReportsMailService regularReportsMailService;

    @Autowired
    ElasticSearchClient elasticSearchClient;

    @Autowired
    RequestBodyService requestBodyService;

    @Autowired
    DateMathStringBuilder dateMathStringBuilder;

    @Value("${regrep.basis.daily}")
    boolean daily;

    @Value("${regrep.basis.weekly}")
    boolean weekly;

    @Value("${regrep.basis.monthly}")
    boolean monthly;

    @Value("${regrep.basis.quarterly}")
    boolean quarterly;

    @Value("${regrep.basis.yearly}")
    boolean yearly;
// Deprecated
//    @Value("${regrep.from}")
//    String from;

    @Value("${regrep.to}")
    String[] to;

    @Value(value = "${app.mail.sender.name}")
    private String mailFromName;

    @Value(value = "${app.mail.sender.email}")
    private String mailFromMail;

    @Value("${regrep.date.pattern}")
    String datePattern;

    @Value("${th.email.message}")
    String msg;

    @Value("${app.es.logs.exc.limit.size}")
    int excLogsLimitSize;

    @Value("${th.email.es.logs.exc.limit.message}")
    String excLogsLimitMessage;

    String parentheses;

    private List<Basis> basisList;

    @PostConstruct
    private void init() {

        // Populate the basis list depending on the placeholder values from the resource file.
        // Service will not send any emails for any basis types having false values.
        basisList = new ArrayList<>();
        parentheses = new String();

        if (daily)
            basisList.add(DAILY);
        if (weekly)
            basisList.add(WEEKLY);
        if (monthly)
            basisList.add(MONTHLY);
        if (quarterly)
            basisList.add(QUARTERLY);
        if (yearly)
            basisList.add(YEARLY);

        LOG.info(String.format("Regular Reports Service Initialized %s", basisList.toString()));

    }

//  @Scheduled(cron="0/12 * * * * ?") // executes every 12 seconds (for testing purposes)
    @Scheduled(cron="${regrep.cron.daily}")
    @Override
    public void reportDaily() {
        if (basisList.contains(DAILY))
          report(DAILY);
    }

    @Scheduled(cron="${regrep.cron.weekly}")
    @Override
    public void reportWeekly() {
        if (basisList.contains(WEEKLY))
          report(WEEKLY);
    }

    @Scheduled(cron="${regrep.cron.monthly}")
    @Override
    public void reportMonthly() {
        if (basisList.contains(MONTHLY))
          report(MONTHLY);
    }

    @Scheduled(cron="${regrep.cron.quarterly}")
    @Override
    public void reportQuarterly() {
        if (basisList.contains(QUARTERLY))
          report(QUARTERLY);
    }

    @Scheduled(cron="${regrep.cron.yearly}")
    @Override
    public void reportYearly() {
        if (basisList.contains(YEARLY))
          report(YEARLY);
    }

    @Override
    public void report(Basis basis) {
        String reportType = basis + " Report";
        boolean isDaily = basis.equals(DAILY) ? true : false;
        LOG.info(String.format("Preparing %s...", reportType));
        String requestBody;
        String gte = new String();
        String lt = new String();
        Map<String, Integer> cspDataResults = new HashMap<>();
        Map<String, Integer> logstashResults = new HashMap<>();
        List<HitsItem> hitsItemList = new ArrayList<>();

        switch (basis) {
            case DAILY: {
                gte = dateMathStringBuilder.buildStringPattern(NOW, MINUS, ONE_DAY, RBTS_OF, DAY);
                lt = dateMathStringBuilder.buildStringPattern(NOW,RBTS_OF,DAY);
                break;
            }
            case WEEKLY: {
                gte = dateMathStringBuilder.buildStringPattern(NOW, MINUS,ONE_WEEK,RBTS_OF,WEEK);
                lt = dateMathStringBuilder.buildStringPattern(NOW,RBTS_OF,WEEK);
                break;
            }
            case MONTHLY: {
                gte = dateMathStringBuilder.buildStringPattern(NOW, MINUS, ONE_MONTH, RBTS_OF, MONTH);
                lt = dateMathStringBuilder.buildStringPattern(NOW,RBTS_OF,MONTH);
                break;
            }
            case QUARTERLY: {
                gte = dateMathStringBuilder.buildStringPattern(NOW, MINUS, THREE_MONTHS, RBTS_OF, MONTH);
                lt = dateMathStringBuilder.buildStringPattern(NOW,RBTS_OF,MONTH);
                break;
            }
            case YEARLY: {
                gte = dateMathStringBuilder.buildStringPattern(NOW,MINUS,ONE_YEAR,RBTS_OF,YEAR);
                lt = dateMathStringBuilder.buildStringPattern(NOW,RBTS_OF,YEAR);
                break;
            }
            default: break;
        }
        LOG.debug("lt = "+ lt);
        //lt = dateMathStringBuilder.buildStringPattern(NOW,RBTS_OF,DAY);

        parentheses = getDates(basis);

        LOG.debug("cspdata request bodies... {");
        for (CspDataMappingType cdmt : CspDataMappingType.values()) {
            if (cdmt != CspDataMappingType.ALL) {  // "all" used for query construction only
                requestBody = requestBodyService.buildRequestBody(gte, lt, cdmt);
                LOG.debug(requestBody);
                cspDataResults.put(cdmt.beautify(), elasticSearchClient.getNdocs(requestBody));
            }
        }
        LOG.debug("\n}");

        LOG.debug("logstash request bodies... {");
        for (LogstashMappingType lmt : LogstashMappingType.values()) {
            if (lmt != LogstashMappingType.ALL) {  // "all" used for query construction only
                requestBody = requestBodyService.buildRequestBody(gte, lt, lmt);
                logstashResults.put(lmt.beautify() + " Logs", elasticSearchClient.getNlogs(requestBody));
            }
        }
        LOG.debug("\n}");

        LOG.debug("Daily logstash request body {");
        if (basis.equals(DAILY)) {
            requestBody = requestBodyService.buildRequestBodyForLogs(gte, lt, LogstashMappingType.EXCEPTION);
            LOG.info(requestBody);
            hitsItemList = elasticSearchClient.getLogData(requestBody);
        }
        LOG.debug("\n}");

        Mail newMail = new Mail();
        newMail.setSenderName(mailFromName);
        newMail.setSenderEmail(mailFromMail);
        newMail.setSubject(reportType);
        newMail.setToArr(to);
        Map valuesMap = new HashMap();

        valuesMap.put("isDaily", isDaily);
        valuesMap.put("thymeleafTypeDescription", "DOCUMENT TYPE");
        valuesMap.put("thymeleafNumberDescription", "CREATED");
        valuesMap.put("recipient", "Administrator");
        valuesMap.put("signature", "Regular Reports Service");
        valuesMap.put("subject", reportType);
        valuesMap.put("message", String.format(msg, reportType, basis.getDescription(), parentheses));
        valuesMap.put("thymeleafMapA", cspDataResults);
        valuesMap.put("thymeleafMapB", logstashResults);
        if (isDaily) {
            valuesMap.put("excLogsList", hitsItemList);
            valuesMap.put("excLogsLimitSize", excLogsLimitSize);
            valuesMap.put("excLogsTotalSize", logstashResults.get("Exception Logs"));
            valuesMap.put("excLogsLimitMessage", String.format(excLogsLimitMessage, excLogsLimitSize, logstashResults.get("Exception Logs")));
        }

        newMail.setModel(valuesMap);

        try {
            regularReportsMailService.sendEmail(newMail);
        } catch (MessagingException e) {
            LOG.error(e.getLocalizedMessage());
        }

    }

    private String getDates(Basis basis) {
        String result = new String();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

        switch (basis) {

            case DAILY: {
                cal.add(Calendar.DATE, -1);
                Date yesterday = cal.getTime();
                result = sdf.format(yesterday);
                break;
            }

            case WEEKLY: {
                cal.add(Calendar.DATE, -1);
                Date dateEnded = cal.getTime();
                cal.add(Calendar.DATE, -7);
                Date dateStarted = cal.getTime();
                //result = String.format("%s - %s", sdf.format(dateStarted), sdf.format(dateEnded));
                result = "Monday - Sunday";
                break;
            }

            case MONTHLY: {
                cal.add(Calendar.MONTH, -1);
                Date lastMonth = cal.getTime();
                sdf = new SimpleDateFormat("MMMM, yyyy");
                result = sdf.format(lastMonth);
                break;
            }

            case QUARTERLY: {
                Date date = new Date();
                sdf = new SimpleDateFormat("MMMM, ");
                for (int i = 1; i <= 3; i++) {
                    cal = Calendar.getInstance();
                    cal.add(Calendar.MONTH, -i);
                    date = cal.getTime();
                    result += sdf.format(date);
                }
                result = result.substring(0, result.length()-2);
                break;
            }

            case YEARLY: {
                cal.add(Calendar.YEAR, -1);
                Date date = cal.getTime();
                sdf = new SimpleDateFormat("yyyy");
                result = sdf.format(date);
                break;
            }

            default: break;

        }

        return String.format("(%s)", result);
    }
}
