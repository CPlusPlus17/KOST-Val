/* == KOST-Tools ================================================================================
 * KOST-Tools. Copyright (C) KOST-CECO. 2012-2021
 * -----------------------------------------------------------------------------------------------
 * KOST-Tools is a development of the KOST-CECO. All rights rest with the KOST-CECO. This
 * application is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. BEDAG AG and Daniel Ludin hereby disclaims all
 * copyright interest in the program SIP-Val v0.2.0 written by Daniel Ludin (BEDAG AG). Switzerland,
 * 1 March 2011. This application is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the follow GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA or see
 * <http://www.gnu.org/licenses/>.
 * ============================================================================================== */

package ch.kostceco.tools.kosttools.fileservice;

import java.io.File;
import java.util.ArrayList;

import ch.kostceco.tools.kosttools.util.Util;
import ch.kostceco.tools.kostval.KOSTVal;
import edu.harvard.hul.ois.jhove.App;
import edu.harvard.hul.ois.jhove.JhoveBase;
import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.OutputHandler;
import edu.harvard.hul.ois.jhove.module.TiffModule;

/** @author Rc Claire Roethlisberger, KOST-CECO */

public class Jhove
{

	public static String valTiff( File tiffFile, File tiffReport, String pathToWorkDir )
	{
		String status = " ??? ";
		try {
			String toplevelDir = tiffFile.getName();
			int lastDotIdx = toplevelDir.lastIndexOf( "." );
			toplevelDir = toplevelDir.substring( 0, lastDotIdx );

			// Vorbereitungen: tiffFile an die JHove Applikation �bergeben

			/* dirOfJarPath damit auch absolute Pfade kein Problem sind Dies ist ein generelles TODO in
			 * allen Modulen. Zuerst immer dirOfJarPath ermitteln und dann alle Pfade mit
			 * 
			 * dirOfJarPath + File.separator +
			 * 
			 * erweitern. */
			String path = new java.io.File(
					KOSTVal.class.getProtectionDomain().getCodeSource().getLocation().getPath() )
							.getAbsolutePath();
			String locationOfJarPath = path;
			String dirOfJarPath = locationOfJarPath;
			if ( locationOfJarPath.endsWith( ".jar" ) || locationOfJarPath.endsWith( ".exe" )
					|| locationOfJarPath.endsWith( "." ) ) {
				File file = new File( locationOfJarPath );
				dirOfJarPath = file.getParent();
			}

			/* Jhove schreibt ins Work-Verzeichnis, damit danach eine Kopie ins Log-Verzeichnis abgelegt
			 * werden kann, welche auch geloescht werden kann. */
			String pathToJhoveOutput = System.getProperty( "java.io.tmpdir" );
			File jhoveReportJTmp =new File( pathToJhoveOutput, tiffFile.getName() + ".jhove-log.txt" );
			if ( jhoveReportJTmp.exists() ) {
				Util.deleteFile( jhoveReportJTmp );
			}
			File jhoveDir = new File( pathToJhoveOutput );
			if ( !jhoveDir.exists() ) {
				jhoveDir.mkdir();
			}

			String pathToJhoveConfig = dirOfJarPath + File.separator + "configuration" + File.separator
					+ "jhove.conf";

			// Jhove direkt ansprechen via dispatch
			try {
				String NAME = new String( "Jhove" );
				String RELEASE = new String( "1.9.2" ); // vom Modul
				int[] DATE = new int[] { 2019, 12, 10 }; // vom Modul yyyy, mm, dd
				String USAGE = new String( "no usage" );
				String RIGHTS = new String( "LGPL v2.1" );
				App app = new App( NAME, RELEASE, DATE, USAGE, RIGHTS );
				JhoveBase je = new JhoveBase();
				OutputHandler handler = je.getHandler( "XML" );

				// check all modules => null oder new TiffModule ();
				Module module = new TiffModule();
				module.init( "" );
				module.setDefaultParams( new ArrayList<String>() );

				String logLevel = null;
				// null = SEVERE, WARNING, INFO, FINE, FINEST
				// Ausgabe in Konsole --> kein Einfluss auf Report
				je.setLogLevel( logLevel );
				String saxClass = null;
				String configFile = pathToJhoveConfig;
				je.init( configFile, saxClass );

				je.setEncoding( "utf-8" );
				je.setTempDirectory( pathToWorkDir + "/jhove" );
				je.setBufferSize( 4096 );
				je.setChecksumFlag( false );
				je.setShowRawFlag( false );
				je.setSignatureFlag( false );
				try {
					Util.switchOffConsole();
					String outputFile = jhoveReportJTmp.getAbsolutePath();
					String[] dirFileOrUri = { tiffFile.getAbsolutePath() };
					je.dispatch( app, module, null, handler, outputFile, dirFileOrUri );
					/* TODO: beim Ausf�hren von je.dispatch gibt es in seltenen F�llen einen Fehler:
					 * 
					 * [Fatal Error] :174:20: Content is not allowed in trailing section.
					 * 
					 * Der Log wird aber korrekt erstellt und der Error kann auch nicht unterdr�ckt werden */
					Util.switchOnConsole();
				} catch ( Exception e ) {
					if ( status.equals( " ??? " ) ) {
						status="Jhove dispatch exception: " + e.getMessage();
					} else {
						status=status+" Jhove dispatch exception: " + e.getMessage();
					}
				}

				// umkopieren, damit es gel�scht werden kann
				File afile = jhoveReportJTmp;
				File bfile = tiffReport;
				Util.copyFile( afile, bfile );
				
			} catch ( Exception e ) {
				if ( status.equals( " ??? " ) ) {
					status="Jhove exception: " + e.getMessage();
				} else {
					status=status+" Jhove exception: " + e.getMessage();
				}
			}
		} catch ( Exception e ) {
			Util.switchOnConsole();
			if ( status.equals( " ??? " ) ) {
				status="Exception fileservice valTiff: " + e.getMessage();
			} else {
				status=status+" Exception fileservice valTiff: " + e.getMessage();
			}
			return status;
		} finally {
			Util.switchOnConsole();
		}
		if ( status.equals( " ??? " ) ) {
			status="OK";
		}
		return status;
	}
}