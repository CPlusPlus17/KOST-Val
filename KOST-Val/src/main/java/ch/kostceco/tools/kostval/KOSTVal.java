/*== KOST-Val ==================================================================================
The KOST-Val v1.2.0 application is used for validate TIFF, SIARD, PDF/A-Files and Submission 
Information Package (SIP). 
Copyright (C) 2012-2014 Claire R�thlisberger (KOST-CECO), Christian Eugster, Olivier Debenath, 
Peter Schneider (Staatsarchiv Aargau), Daniel Ludin (BEDAG AG)
-----------------------------------------------------------------------------------------------
KOST-Val is a development of the KOST-CECO. All rights rest with the KOST-CECO. 
This application is free software: you can redistribute it and/or modify it under the 
terms of the GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version. 
BEDAG AG and Daniel Ludin hereby disclaims all copyright interest in the program 
SIP-Val v0.2.0 written by Daniel Ludin (BEDAG AG). Switzerland, 1 March 2011.
This application is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the follow GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program; 
if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
Boston, MA 02110-1301 USA or see <http://www.gnu.org/licenses/>.
==============================================================================================*/

package ch.kostceco.tools.kostval;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.kostceco.tools.kostval.controller.Controllersip;
import ch.kostceco.tools.kostval.controller.Controllertiff;
import ch.kostceco.tools.kostval.controller.Controllersiard;
import ch.kostceco.tools.kostval.controller.Controllerpdfa;
import ch.kostceco.tools.kostval.logging.LogConfigurator;
import ch.kostceco.tools.kostval.logging.Logger;
import ch.kostceco.tools.kostval.logging.MessageConstants;
import ch.kostceco.tools.kostval.service.ConfigurationService;
import ch.kostceco.tools.kostval.service.TextResourceService;
import ch.kostceco.tools.kostval.util.Util;
import ch.kostceco.tools.kostval.util.Zip64Archiver;

/**
 * Dies ist die Starter-Klasse, verantwortlich f�r das Initialisieren des
 * Controllers, des Loggings und das Parsen der Start-Parameter.
 * 
 * @author Rc Claire R�thlisberger, KOST-CECO
 */
public class KOSTVal implements MessageConstants
{

	private static final Logger		LOGGER	= new Logger( KOSTVal.class );

	private TextResourceService		textResourceService;
	private ConfigurationService	configurationService;

	public TextResourceService getTextResourceService()
	{
		return textResourceService;
	}

	public void setTextResourceService( TextResourceService textResourceService )
	{
		this.textResourceService = textResourceService;
	}

	public ConfigurationService getConfigurationService()
	{
		return configurationService;
	}

	public void setConfigurationService(
			ConfigurationService configurationService )
	{
		this.configurationService = configurationService;
	}

	/**
	 * Die Eingabe besteht aus 2 oder 3 Parameter: [0] Validierungstyp [1] Pfad
	 * zur Val-File [2] option: Verbose
	 * 
	 * @param args
	 */
	@SuppressWarnings("unused")
	public static void main( String[] args )
	{
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:config/applicationContext.xml" );

		// Zeitstempel Start
		java.util.Date nowStart = new java.util.Date();
		java.text.SimpleDateFormat sdfStart = new java.text.SimpleDateFormat(
				"dd.MM.yyyy HH.mm.ss" );
		String ausgabeStart = sdfStart.format( nowStart );

		// TODO: siehe Bemerkung im applicationContext-services.xml bez�glich
		// Injection in der Superklasse aller Impl-Klassen
		// ValidationModuleImpl validationModuleImpl = (ValidationModuleImpl)
		// context.getBean("validationmoduleimpl");

		KOSTVal kostval = (KOSTVal) context.getBean( "kostval" );

		// Ist die Anzahl Parameter (mind. 2) korrekt?
		if ( args.length < 2 ) {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_PARAMETER_USAGE ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
			System.exit( 1 );
		}

		// Informationen zum Arbeitsverzeichnis holen
		String pathToWorkDir = kostval.getConfigurationService()
				.getPathToWorkDir();
		/*
		 * Nicht vergessen in
		 * "src/main/resources/config/applicationContext-services.xml" beim
		 * entsprechenden Modul die property anzugeben: <property
		 * name="configurationService" ref="configurationService" />
		 */

		// Informationen holen, welche Formate validiert werden sollen
		String pdfaValidation = kostval.getConfigurationService()
				.pdfaValidation();
		String siardValidation = kostval.getConfigurationService()
				.siardValidation();
		String tiffValidation = kostval.getConfigurationService()
				.tiffValidation();

		File tmpDir = new File( pathToWorkDir );

		// bestehendes Workverzeichnis ggf. l�schen
		if ( tmpDir.exists() ) {
			Util.deleteDir( tmpDir );
		}
		if ( tmpDir.exists() ) {
			tmpDir.delete();
		}

		// die Anwendung muss mindestens unter Java 6 laufen
		String javaRuntimeVersion = System.getProperty( "java.vm.version" );
		if ( javaRuntimeVersion.compareTo( "1.6.0" ) < 0 ) {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_WRONG_JRE ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
			System.exit( 1 );
		}

		// Ueberpr�fung des Parameters (Log-Verzeichnis)
		String pathToLogfile = kostval.getConfigurationService()
				.getPathToLogfile();

		File directoryOfLogfile = new File( pathToLogfile );
		File directoryOfLogfileParent1 = directoryOfLogfile.getParentFile();

		if ( !directoryOfLogfile.exists() ) {
			if ( !directoryOfLogfileParent1.exists() ) {
				directoryOfLogfileParent1.mkdir();
			}
			directoryOfLogfile.mkdir();
		}

		// Im Logverzeichnis besteht kein Schreibrecht
		if ( !directoryOfLogfile.canWrite() ) {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_LOGDIRECTORY_NOTWRITABLE, directoryOfLogfile ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
			System.exit( 1 );
		}

		if ( !directoryOfLogfile.isDirectory() ) {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_LOGDIRECTORY_NODIRECTORY ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
			System.exit( 1 );
		}

		// bestehendes Workverzeichnis ggf. l�schen und wieder anlegen
		if ( tmpDir.exists() ) {
			Util.deleteDir( tmpDir );
		}
		if ( tmpDir.exists() ) {
			tmpDir.delete();
		}

		if ( !tmpDir.exists() ) {
			tmpDir.mkdir();
		}

		// Im workverzeichnis besteht kein Schreibrecht
		if ( !tmpDir.canWrite() ) {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_WORKDIRECTORY_NOTWRITABLE, tmpDir ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
			System.exit( 1 );
		}

		// Ueberpr�fung des optionalen Parameters (2 -v --> im Verbose-mode
		// werden die originalen Logs nicht gel�scht (PDFTron, Jhove & Co.)
		boolean verbose = false;
		if ( args.length > 2 ) {
			if ( !(args[2].equals( "-v" )) ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						ERROR_PARAMETER_OPTIONAL_1 ) );
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_INTERRUPTED ) );
				System.exit( 1 );
			} else {
				verbose = true;
			}
		}

		// Initialisierung TIFF-Modul B (JHove-Validierung)
		// �berpr�fen der Konfiguration: existiert die jhove.conf am
		// angebenen Ort?
		String jhoveConf = kostval.getConfigurationService()
				.getPathToJhoveConfiguration();
		File fJhoveConf = new File( jhoveConf );
		if ( !fJhoveConf.exists()
				|| !fJhoveConf.getName().equals( "jhove.conf" ) ) {

			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_JHOVECONF_MISSING ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
			System.exit( 1 );
		}

		File valDatei = new File( args[1] );
		File logDatei = null;
		logDatei = valDatei;

		// Konfiguration des Loggings, ein File Logger wird
		// zus�tzlich erstellt
		LogConfigurator logConfigurator = (LogConfigurator) context
				.getBean( "logconfigurator" );
		String logFileName = logConfigurator.configure(
				directoryOfLogfile.getAbsolutePath(), logDatei.getName() );
		LOGGER.logInfo( kostval.getTextResourceService().getText(
				MESSAGE_KOSTVALIDATION ) );

		// Ueberpr�fung des Parameters (Val-Datei): existiert die Datei?
		if ( !valDatei.exists() ) {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_VALFILE_FILENOTEXISTING ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
			System.exit( 1 );
		}

		if ( args[0].equals( "--format" ) ) {

			Integer countNio = 0;
			Integer countSummaryNio = 0;
			Integer count = 0;
			Integer pdfaCountIo = 0;
			Integer pdfaCountNio = 0;
			Integer siardCountIo = 0;
			Integer siardCountNio = 0;
			Integer tiffCountIo = 0;
			Integer tiffCountNio = 0;

			// TODO: Formatvalidierung an einer Datei
			// --> erledigt --> nur Marker
			if ( !valDatei.isDirectory() ) {

				boolean valFile = valFile( valDatei, logFileName,
						directoryOfLogfile, verbose );

				// L�schen des Arbeitsverzeichnisses, falls eines
				// angelegt wurde
				if ( tmpDir.exists() ) {
					Util.deleteDir( tmpDir );
				}
				if ( valFile ) {
					// L�schen des Arbeitsverzeichnisses, falls eines
					// angelegt wurde
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					// Validierte Datei valide
					System.exit( 0 );
				} else {
					// L�schen des Arbeitsverzeichnisses, falls eines
					// angelegt wurde
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					// Fehler in Validierte Datei --> invalide
					System.exit( 2 );

				}
			} else {
				// TODO: Formatvalidierung �ber ein Ordner
				// --> erledigt --> nur Marker
				Map<String, File> fileMap = Util.getFileMap( valDatei, false );
				Set<String> fileMapKeys = fileMap.keySet();
				for ( Iterator<String> iterator = fileMapKeys.iterator(); iterator
						.hasNext(); ) {
					String entryName = iterator.next();
					File newFile = fileMap.get( entryName );
					if ( !newFile.isDirectory() ) {
						valDatei = newFile;
						count = count + 1;

						if ( ((valDatei.getAbsolutePath().toLowerCase()
								.endsWith( ".tiff" ) || valDatei
								.getAbsolutePath().toLowerCase()
								.endsWith( ".tif" )))
								&& tiffValidation.equals( "yes" ) ) {

							boolean valFile = valFile( valDatei, logFileName,
									directoryOfLogfile, verbose );

							// L�schen des Arbeitsverzeichnisses, falls eines
							// angelegt wurde
							if ( tmpDir.exists() ) {
								Util.deleteDir( tmpDir );
							}
							if ( valFile ) {
								tiffCountIo = tiffCountIo + 1;
								// L�schen des Arbeitsverzeichnisses, falls
								// eines angelegt wurde
								if ( tmpDir.exists() ) {
									Util.deleteDir( tmpDir );
								}
							} else {
								tiffCountNio = tiffCountNio + 1;
								// L�schen des Arbeitsverzeichnisses, falls
								// eines angelegt wurde
								if ( tmpDir.exists() ) {
									Util.deleteDir( tmpDir );
								}
							}
						} else if ( (valDatei.getAbsolutePath().toLowerCase()
								.endsWith( ".siard" ))
								&& siardValidation.equals( "yes" ) ) {

							boolean valFile = valFile( valDatei, logFileName,
									directoryOfLogfile, verbose );

							// L�schen des Arbeitsverzeichnisses, falls eines
							// angelegt wurde
							if ( tmpDir.exists() ) {
								Util.deleteDir( tmpDir );
							}
							if ( valFile ) {
								siardCountIo = siardCountIo + 1;
								// L�schen des Arbeitsverzeichnisses, falls
								// eines angelegt wurde
								if ( tmpDir.exists() ) {
									Util.deleteDir( tmpDir );
								}
							} else {
								siardCountNio = siardCountNio + 1;
								// L�schen des Arbeitsverzeichnisses, falls
								// eines angelegt wurde
								if ( tmpDir.exists() ) {
									Util.deleteDir( tmpDir );
								}
							}

						} else if ( ((valDatei.getName().endsWith( ".pdf" ) || valDatei
								.getName().endsWith( ".pdfa" )))
								&& pdfaValidation.equals( "yes" ) ) {

							boolean valFile = valFile( valDatei, logFileName,
									directoryOfLogfile, verbose );

							// L�schen des Arbeitsverzeichnisses, falls eines
							// angelegt wurde
							if ( tmpDir.exists() ) {
								Util.deleteDir( tmpDir );
							}
							if ( valFile ) {
								pdfaCountIo = pdfaCountIo + 1;
								// L�schen des Arbeitsverzeichnisses, falls
								// eines angelegt wurde
								if ( tmpDir.exists() ) {
									Util.deleteDir( tmpDir );
								}
							} else {
								pdfaCountNio = pdfaCountNio + 1;
								// L�schen des Arbeitsverzeichnisses, falls
								// eines angelegt wurde
								if ( tmpDir.exists() ) {
									Util.deleteDir( tmpDir );
								}
							}

						} else {
							countNio = countNio + 1;
						}
					}
				}
				// Zeitstempel End
				java.util.Date nowEnd = new java.util.Date();
				java.text.SimpleDateFormat sdfEnd = new java.text.SimpleDateFormat(
						"dd.MM.yyyy HH.mm.ss" );
				String ausgabeEnd = sdfEnd.format( nowEnd );

				/*
				 * *************************************************************
				 * * Zusammenfassung der Formatvalidierung *
				 * ===================================== * Total: = count{0} * *
				 * PDF/A: Valid = countPdfaIo{1} Invalid = countPdfaNio{2} *
				 * SIARD: Valid = countSiardIo{3} Invalid = countSiardNio{4} *
				 * TIFF: Valid = countTiffIo{5} Invalid = countTiffNio{6} * *
				 * Sonstige Dateien: countNio{7} (ohne Formatvalidierung)
				 * Gesamtergebnis: totalSummary{10} Validierungszeitraum:
				 * ausgabeStart {8} - ausgabeEnd{9}
				 * *************************************************************
				 */
				countSummaryNio = pdfaCountNio + siardCountNio + tiffCountNio;
				String totalSummary = null;
				if ( countSummaryNio == 0 ) {
					totalSummary = "Valid";
				} else {
					totalSummary = "Invalid";
				}

				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_FOOTER_SUMMARY_1, count ) );
				if ( pdfaValidation.equals( "yes" ) ) {
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							MESSAGE_FOOTER_SUMMARY_PDFA, pdfaCountIo,
							pdfaCountNio ) );
				}
				if ( siardValidation.equals( "yes" ) ) {
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							MESSAGE_FOOTER_SUMMARY_SIARD, siardCountIo,
							siardCountNio ) );
				}
				if ( tiffValidation.equals( "yes" ) ) {
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							MESSAGE_FOOTER_SUMMARY_TIFF, tiffCountIo,
							tiffCountNio ) );
				}
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_FOOTER_SUMMARY_2, countNio, ausgabeStart,
						ausgabeEnd, totalSummary ) );

				if ( countNio == count ) {
					// keine Dateien Validiert
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							ERROR_INCORRECTFILEENDING ) );
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							MESSAGE_VALIDATION_INTERRUPTED ) );

					// bestehendes Workverzeichnis ggf. l�schen
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					System.exit( 1 );
				} else if ( countSummaryNio == 0 ) {
					// bestehendes Workverzeichnis ggf. l�schen
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					// alle Validierten Dateien valide
					System.exit( 0 );
				} else {
					// bestehendes Workverzeichnis ggf. l�schen
					if ( tmpDir.exists() ) {
						Util.deleteDir( tmpDir );
					}
					// Fehler in Validierten Dateien --> invalide
					System.exit( 2 );
				}
			}
		} else if ( args[0].equals( "--sip" ) ) {
			// TODO: Sipvalidierung
			// --> erledigt --> nur Marker
			boolean validFormat = false;
			File originalSipFile = valDatei;
			File outputFile3c = null;
			String fileName3c = null;
			File tmpDirZip = null;

			// zuerst eine Formatvalidierung �ber den Content
			// dies ist analog aufgebaut wie --format
			Integer countNio = 0;
			Integer countSummaryNio = 0;
			Integer count = 0;
			Integer pdfaCountIo = 0;
			Integer pdfaCountNio = 0;
			Integer siardCountIo = 0;
			Integer siardCountNio = 0;
			Integer tiffCountIo = 0;
			Integer tiffCountNio = 0;

			if ( !valDatei.isDirectory() ) {
				// geziptes SIP --> in temp dir entzipen
				tmpDirZip = new File( tmpDir.getAbsolutePath() + "\\ZIP" );

				try {
					Zip64Archiver.unzip( valDatei, tmpDirZip );
					valDatei = tmpDirZip;

				} catch ( Exception e ) {
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							ERROR_MODULE_AA_INCORRECTFILEENDING ) );
				}
			}
			Map<String, File> fileMap = Util.getFileMap( valDatei, false );
			Set<String> fileMapKeys = fileMap.keySet();
			for ( Iterator<String> iterator = fileMapKeys.iterator(); iterator
					.hasNext(); ) {
				String entryName = iterator.next();
				File newFile = fileMap.get( entryName );

				if ( !newFile.isDirectory()
						&& newFile.getAbsolutePath().contains( "\\content\\" ) ) {
					valDatei = newFile;
					count = count + 1;

					if ( ((valDatei.getAbsolutePath().toLowerCase()
							.endsWith( ".tiff" ) || valDatei.getAbsolutePath()
							.toLowerCase().endsWith( ".tif" )))
							&& tiffValidation.equals( "yes" ) ) {

						boolean valFile = valFile( valDatei, logFileName,
								directoryOfLogfile, verbose );

						if ( valFile ) {
							tiffCountIo = tiffCountIo + 1;
						} else {
							tiffCountNio = tiffCountNio + 1;
						}

					} else if ( (valDatei.getAbsolutePath().toLowerCase()
							.endsWith( ".siard" ))
							&& siardValidation.equals( "yes" ) ) {

						boolean valFile = valFile( valDatei, logFileName,
								directoryOfLogfile, verbose );

						if ( valFile ) {
							siardCountIo = siardCountIo + 1;
						} else {
							siardCountNio = siardCountNio + 1;
						}

					} else if ( ((valDatei.getName().endsWith( ".pdf" ) || valDatei
							.getName().endsWith( ".pdfa" )))
							&& pdfaValidation.equals( "yes" ) ) {

						boolean valFile = valFile( valDatei, logFileName,
								directoryOfLogfile, verbose );

						if ( valFile ) {
							pdfaCountIo = pdfaCountIo + 1;
						} else {
							pdfaCountNio = pdfaCountNio + 1;
						}

					} else {
						countNio = countNio + 1;
					}
					tmpDirZip = new File( tmpDir.getAbsolutePath() + "\\ZIP" );
					if ( newFile.getAbsolutePath().contains(
							tmpDirZip.getAbsolutePath() ) ) {
						// newFile ist eine TempZip-Datei und kann gel�scht
						// werden
						try {
							StringBuffer command = new StringBuffer(
									"cmd /c ping -n 120 127.0.0.1 > NUL && del "
											+ newFile.getAbsolutePath() );
							Runtime rt = Runtime.getRuntime();
							Process proc = rt.exec( command.toString() );
						} catch ( IOException e ) {
							e.printStackTrace();
							System.out
									.println( newFile.getAbsolutePath()
											+ " konnte auch nicht durch neue cmd geloescht werden. Bitte manuell loeschen." );

						}
					}
				}
			}
			// Zeitstempel End
			java.util.Date nowEnd = new java.util.Date();
			java.text.SimpleDateFormat sdfEnd = new java.text.SimpleDateFormat(
					"dd.MM.yyyy HH.mm.ss" );
			String ausgabeEnd = sdfEnd.format( nowEnd );

			/*
			 * *************************************************************
			 * * Zusammenfassung der Formatvalidierung *
			 * ===================================== * Total: = count{0} * *
			 * PDF/A: Valid = countPdfaIo{1} Invalid = countPdfaNio{2} * SIARD:
			 * Valid = countSiardIo{3} Invalid = countSiardNio{4} * TIFF: Valid
			 * = countTiffIo{5} Invalid = countTiffNio{6} * * Sonstige Dateien:
			 * countNio{7} (ohne Formatvalidierung) Gesamtergebnis:
			 * totalSummary{10} Validierungszeitraum: ausgabeStart {8} -
			 * ausgabeEnd{9}
			 * *************************************************************
			 */
			countSummaryNio = pdfaCountNio + siardCountNio + tiffCountNio;
			String totalSummary = null;
			if ( countSummaryNio == 0 ) {
				totalSummary = "Valid";
			} else {
				totalSummary = "Invalid";
			}

			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_SUMMARY_1, count ) );
			if ( pdfaValidation.equals( "yes" ) ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_FOOTER_SUMMARY_PDFA, pdfaCountIo, pdfaCountNio ) );
			}
			if ( siardValidation.equals( "yes" ) ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_FOOTER_SUMMARY_SIARD, siardCountIo,
						siardCountNio ) );
			}
			if ( tiffValidation.equals( "yes" ) ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_FOOTER_SUMMARY_TIFF, tiffCountIo, tiffCountNio ) );
			}
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_SUMMARY_2, countNio, ausgabeStart,
					ausgabeEnd, totalSummary ) );

			if ( countNio == count ) {
				// keine Dateien Validiert --> keine Invalide
				validFormat = true;
				fileName3c = "3c_Valide.txt";
			} else if ( countSummaryNio == 0 ) {
				// alle Validierten Dateien valide
				validFormat = true;
				fileName3c = "3c_Valide.txt";
			} else {
				// Fehler in Validierten Dateien --> invalide
				validFormat = false;
				fileName3c = "3c_Invalide.txt";
			}
			outputFile3c = new File( directoryOfLogfile + fileName3c );
			try {
				outputFile3c.createNewFile();
			} catch ( IOException e ) {
				e.printStackTrace();
			}

			// Start Normale SIP-Validierung mit auswertung Format-Val. im 3c

			valDatei = originalSipFile;
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_SIPVALIDATION, valDatei.getName() ) );
			File targetFile = new File( pathToWorkDir + "\\SIP-Validierung",
					valDatei.getName() + ".zip" );
			File tmpDirSip = new File( pathToWorkDir + "\\SIP-Validierung" );

			// Ueberpr�fung des 1. Parameters (SIP-Datei): ist die Datei ein
			// Verzeichnis? Wenn ja, wird im work-Verzeichnis eine Zip-Datei
			// daraus erstellt, damit die weiteren Validierungen Gebrauch von
			// der java.util.zip API machen k�nnen, und somit die zu
			// Validierenden Archive gleichartig behandelt werden k�nnen, egal
			// ob es sich um eine Verzeichnisstruktur oder ein Zip-File handelt.
			// Informationen zum Arbeitsverzeichnis holen

			String originalSipName = valDatei.getAbsolutePath();
			if ( valDatei.isDirectory() ) {
				if ( tmpDirSip.exists() ) {
					Util.deleteDir( tmpDirSip );
				}
				tmpDirSip.mkdir();

				try {
					Zip64Archiver.archivate( valDatei, targetFile );
					valDatei = targetFile;

				} catch ( Exception e ) {
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							ERROR_CANNOTCREATEZIP ) );
					System.exit( 1 );
				}

			} else {
				// L�schen des targetFile, falls eines angelegt wurde
				if ( targetFile.exists() ) {
					Util.deleteDir( targetFile );
				}
				targetFile.mkdir();
			}
			Controllersip controller = (Controllersip) context
					.getBean( "controllersip" );
			boolean okMandatory = false;
			okMandatory = controller.executeMandatory( valDatei,
					directoryOfLogfile );
			boolean ok = false;

			// die Validierungen 1a - 1d sind obligatorisch, wenn sie bestanden
			// wurden, k�nnen die restlichen
			// Validierungen, welche nicht zum Abbruch der Applikation f�hren,
			// ausgef�hrt werden.
			if ( okMandatory ) {
				ok = controller.executeOptional( valDatei, directoryOfLogfile );
			}
			// Formatvalidierung validFormat
			ok = (ok && okMandatory && validFormat);

			LOGGER.logInfo( "" );
			if ( ok ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_TOTAL_VALID, valDatei.getAbsolutePath() ) );
			} else {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_TOTAL_INVALID, valDatei.getAbsolutePath() ) );
			}
			LOGGER.logInfo( "" );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_SIP, originalSipName ) );

			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_LOG, logFileName ) );
			LOGGER.logInfo( "" );

			if ( okMandatory ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_FINISHED ) );
			} else {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_INTERRUPTED ) );
			}

			// L�schen des targetFile, falls eines angelegt wurde
			if ( targetFile.exists() ) {
				Util.deleteDir( targetFile );
			}
			// bestehendes Workverzeichnis ggf. l�schen
			if ( tmpDir.exists() ) {
				Util.deleteDir( tmpDir );
			}
			StringBuffer command = new StringBuffer( "rd "
					+ tmpDir.getAbsolutePath() + " /s /q" );
			if ( ok ) {
				if ( tmpDir.exists() ) {

					try {
						Runtime rt = Runtime.getRuntime();
						Process proc = rt.exec( command.toString() );
					} catch ( IOException e ) {
						System.out.println( "" );
						System.out.println( tmpDir.getAbsolutePath() );
						System.out
								.println( "... konnte nicht geloescht werden. Bitte manuell loeschen." );
						System.out
								.println( "... n`a pas pu etre supprime. S`il vous plait supprimer manuellement." );
						System.out.println( "" );
					}
				}
				System.exit( 0 );
			} else {
				if ( tmpDir.exists() ) {
					try {
						Runtime rt = Runtime.getRuntime();
						Process proc = rt.exec( command.toString() );
					} catch ( IOException e ) {
						System.out.println( "" );
						System.out.println( tmpDir.getAbsolutePath() );
						System.out
								.println( "... konnte nicht geloescht werden. Bitte manuell loeschen." );
						System.out
								.println( "... n`a pas pu etre supprime. S`il vous plait supprimer manuellement." );
						System.out.println( "" );
					}
				}
				System.exit( 2 );
			}
		} else {
			// Ueberpr�fung des Parameters (Val-Typ): format / sip
			// args[0] ist nicht "--format" oder "--sip" --> INVALIDE
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_PARAMETER_USAGE ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
			if ( tmpDir.exists() ) {
				Util.deleteDir( tmpDir );
				tmpDir.deleteOnExit();
			}
			System.exit( 1 );
		}
	}

	// TODO: ValFile --> Formatvalidierung einer Datei
	// --> erledigt --> nur Marker
	private static boolean valFile( File valDatei, String logFileName,
			File directoryOfLogfile, boolean verbose )
	{
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"classpath:config/applicationContext.xml" );

		KOSTVal kostval = (KOSTVal) context.getBean( "kostval" );
		String originalValName = valDatei.getAbsolutePath();
		boolean valFile = false;

		if ( (valDatei.getAbsolutePath().toLowerCase().endsWith( ".tiff" ) || valDatei
				.getAbsolutePath().toLowerCase().endsWith( ".tif" )) ) {

			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_TIFFVALIDATION, valDatei.getName() ) );
			Controllertiff controller1 = (Controllertiff) context
					.getBean( "controllertiff" );
			boolean okMandatory = controller1.executeMandatory( valDatei,
					directoryOfLogfile );
			boolean ok = false;

			// die Validierungen A sind obligatorisch, wenn sie
			// bestanden wurden, k�nnen die restlichen
			// Validierungen, welche nicht zum Abbruch der
			// Applikation f�hren, ausgef�hrt werden.
			if ( okMandatory ) {
				ok = controller1.executeOptional( valDatei, directoryOfLogfile );
			}

			ok = (ok && okMandatory);
			valFile = ok;

			LOGGER.logInfo( "" );
			if ( ok ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_TOTAL_VALID, valDatei.getAbsolutePath() ) );
			} else {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_TOTAL_INVALID, valDatei.getAbsolutePath() ) );
			}
			LOGGER.logInfo( "" );

			// Ausgabe der Pfade zu den Jhove Reports, falls welche
			// generiert wurden (-v) oder Jhove Report l�schen
			File jhoveReport = new File( directoryOfLogfile, valDatei.getName()
					+ ".jhove-log.txt" );

			if ( jhoveReport.exists() ) {
				if ( verbose ) {
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							MESSAGE_FOOTER_REPORTJHOVE,
							Util.getPathToReportJHove() ) );
					LOGGER.logInfo( "" );
				} else {
					// kein optionaler Parameter --> Jhove-Report loeschen!
					jhoveReport.delete();
				}
			}

			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_TIFF, originalValName ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_LOG, logFileName ) );
			LOGGER.logInfo( "" );

			if ( okMandatory ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_FINISHED ) );
			} else {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_INTERRUPTED ) );
			}

		} else if ( (valDatei.getAbsolutePath().toLowerCase()
				.endsWith( ".siard" )) ) {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_SIARDVALIDATION, valDatei.getName() ) );
			Controllersiard controller2 = (Controllersiard) context
					.getBean( "controllersiard" );
			boolean okMandatory = controller2.executeMandatory( valDatei,
					directoryOfLogfile );
			boolean ok = false;

			// die Validierungen A-D sind obligatorisch, wenn sie
			// bestanden wurden, k�nnen die restlichen
			// Validierungen, welche nicht zum Abbruch der
			// Applikation f�hren, ausgef�hrt werden.
			if ( okMandatory ) {
				ok = controller2.executeOptional( valDatei, directoryOfLogfile );
				// Ausf�hren der optionalen Schritte
			}

			ok = (ok && okMandatory);
			valFile = ok;

			LOGGER.logInfo( "" );
			if ( ok ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_TOTAL_VALID, valDatei.getAbsolutePath() ) );
			} else {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_TOTAL_INVALID, valDatei.getAbsolutePath() ) );
			}
			LOGGER.logInfo( "" );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_SIARD, originalValName ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_LOG, logFileName ) );
			LOGGER.logInfo( "" );

			if ( okMandatory ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_FINISHED ) );
			} else {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_INTERRUPTED ) );
			}

		} else if ( (valDatei.getName().endsWith( ".pdf" ) || valDatei
				.getName().endsWith( ".pdfa" )) ) {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_PDFAVALIDATION, valDatei.getName() ) );
			Controllerpdfa controller3 = (Controllerpdfa) context
					.getBean( "controllerpdfa" );
			boolean okMandatory = controller3.executeMandatory( valDatei,
					directoryOfLogfile );
			boolean ok = false;

			// die Validierung A ist obligatorisch, wenn sie
			// bestanden wurden, k�nnen die restlichen
			// Validierungen, welche nicht zum Abbruch der
			// Applikation f�hren, ausgef�hrt werden.
			if ( okMandatory ) {
				ok = controller3.executeOptional( valDatei, directoryOfLogfile );
				// Ausf�hren der optionalen Schritte
			}

			ok = (ok && okMandatory);
			valFile = ok;

			LOGGER.logInfo( "" );
			if ( ok ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_TOTAL_VALID, valDatei.getAbsolutePath() ) );
			} else {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_TOTAL_INVALID, valDatei.getAbsolutePath() ) );
			}
			LOGGER.logInfo( "" );

			// Ausgabe der Pfade zu den Pdftron Reports, falls welche
			// generiert wurden (-v) oder Pdftron Reports l�schen
			File pdftronReport = new File( directoryOfLogfile,
					valDatei.getName() + ".pdftron-log.xml" );
			File pdftronXsl = new File( directoryOfLogfile, "report.xsl" );

			if ( pdftronReport.exists() ) {
				if ( verbose ) {
					LOGGER.logInfo( kostval.getTextResourceService().getText(
							MESSAGE_FOOTER_REPORTPDFTRON,
							Util.getPathToReportPdftron() ) );
					LOGGER.logInfo( "" );
				} else {
					// kein optionaler Parameter --> PDFTron-Report loeschen!
					pdftronReport.delete();
					pdftronXsl.delete();
				}
			}

			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_PDFA, originalValName ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_FOOTER_LOG, logFileName ) );
			LOGGER.logInfo( "" );

			if ( okMandatory ) {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_FINISHED ) );
			} else {
				LOGGER.logInfo( kostval.getTextResourceService().getText(
						MESSAGE_VALIDATION_INTERRUPTED ) );
			}

		} else {
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					ERROR_INCORRECTFILEENDING ) );
			LOGGER.logInfo( kostval.getTextResourceService().getText(
					MESSAGE_VALIDATION_INTERRUPTED ) );
		}
		return valFile;
	}

}
