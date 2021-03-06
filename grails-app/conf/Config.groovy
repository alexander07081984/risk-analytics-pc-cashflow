import org.pillarone.riskanalytics.core.output.batch.results.MysqlBulkInsert
import org.pillarone.riskanalytics.core.output.batch.calculations.MysqlCalculationsBulkInsert
import org.pillarone.riskanalytics.core.output.batch.calculations.GenericBulkInsert

// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.types = [html: ['text/html', 'application/xhtml+xml'],
        xml: ['text/xml', 'application/xml'],
        text: 'text-plain',
        js: 'text/javascript',
        rss: 'application/rss+xml',
        atom: 'application/atom+xml',
        css: 'text/css',
        csv: 'text/csv',
        all: '*/*',
        json: ['application/json', 'text/json'],
        form: 'application/x-www-form-urlencoded',
        multipartForm: 'multipart/form-data'
]
// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
grails.doc.images = new File('src/docs/images')
grails.doc.css = new File('src/docs/css')
grails.doc.style = new File('src/docs/style')


maxIterations = 100000
keyFiguresToCalculate = null
resultBulkInsert = null
userLogin = false
// a cron for a batch, A cron expression is a string comprised of 6 or 7 fields separated by white space.
// Fields can contain any of the allowed values: Sec Min Hour dayOfMonth month dayOfWeek Year
// Fire every 60 minutes
batchCron = "0 0/10 * * * ?"
environments {

    development {
        ExceptionSafeOut = System.out
        log4j = {
            info 'org.pillarone.riskanalytics.core.output',
                    'org.pillarone.riskanalytics.core.components',
                    'org.pillarone.riskanalytics.core.simulation',
                    'org.pillarone.modelling.fileimport',
                    'org.pillarone.modelling.domain',
                    'org.pillarone.modelling.packets',
                    'org.pillarone.riskanalytics.core.simulation.engine',
                    'org.pillarone.riskanalytics.core.parameterization'

            debug 'org.pillarone.modelling.output',
                    'org.pillarone.riskanalytics.domain.life.reinsurance.UnitLinkedLifeReinsuranceContractPacket',
                    'org.pillarone.riskanalytics.domain.pc.cf'

            warn()
        }
        keyFiguresToCalculate = [
                'stdev': true,
                'percentile': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'var': [99, 99.5],
                'tvar': [99, 99.5],
                'pdf': 200
        ]
    }
    test {
        ExceptionSafeOut = System.out
        resultBulkInsert = org.pillarone.riskanalytics.core.output.batch.results.GenericBulkInsert
        calculationBulkInsert = GenericBulkInsert
        grails.project.compile.verbose=true
        keyFiguresToCalculate = [
                'stdev': true,
                'percentile': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'var': [99, 99.5],
                'tvar': [99, 99.5],
                'pdf': 200
        ]
        log4j = {
            appenders {
                console name: 'stdout', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
                file name: 'file', file: 'RiskAnalytics.log', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
            }
            root {
                error 'stdout', 'file'
                additivity = false
            }
            info()
            debug()

            warn 'org.grails.plugins.excelimport.DefaultImportCellCollector'
        }
    }
    mysql {
        resultBulkInsert = MysqlBulkInsert
        calculationBulkInsert = MysqlCalculationsBulkInsert
        ExceptionSafeOut = System.out
        models = ["PodraPModel"]
        log4j = {
            appenders {
                console name: 'stdout', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
                file name: 'file', file: 'RiskAnalytics.log', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
            }
            root {
                error 'stdout', 'file'
                additivity = false
            }
            info 'org.pillarone.riskanalytics.core.output',
                    'org.pillarone.riskanalytics.core.components',
                    'org.pillarone.riskanalytics.core.simulation',
                    'org.pillarone.modelling.fileimport',
                    'org.pillarone.modelling.domain',
                    'org.pillarone.modelling.packets',
                    'org.pillarone.riskanalytics.core.parameterization',
                    'org.pillarone.application.jobs.JobScheduler',
                    'org.pillarone.riskanalytics.core.simulation.engine',
                    'org.pillarone.application.jobs.BatchRunner',
                    'org.pillarone.modelling.ui.main.action.ImportAllAction',
                    'org.pillarone.modelling.ui.main.action.ItemLoadHandler'

            debug 'org.pillarone.riskAnalytics.output',
                    'org.pillarone.riskanalytics.core.simulation.engine.actions',
                    'org.pillarone.riskanalytics.domain.pc.cf'

            warn()
        }
        keyFiguresToCalculate = [
                'stdev': true,
                'percentile': [0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0],
                'var': [99, 99.5],
                'tvar': [99, 99.5],
                'pdf': 200
        ]
    }
}

log4j = {
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
        file name: 'file', file: 'RiskAnalytics.log', layout: pattern(conversionPattern: '[%d] %-5p %c{1} %m%n')
    }
    root {
        error 'stdout', 'file'
        additivity = true
    }
    error 'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
//        'org.springframework',
//        'org.hibernate',
            'org.pillarone.modelling.fileimport',
            'org.pillarone.modelling.ui.util.ExceptionSafe',
            'org.pillarone.riskanalytics.core.wiring',
            'org.pillarone.modelling.domain',
            'org.pillarone.modelling.util',
    info()
    debug 'org.pillarone.riskanalytics.domain.pc.cf'
    warn()
}
// Uncomment and edit the following lines to start using Grails encoding & escaping improvements

/* remove this line 
// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside null
                scriptlet = 'none' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}
remove this line */
