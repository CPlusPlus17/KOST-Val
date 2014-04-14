/*== KOST-Val ==================================================================================
The KOST-Val application is used for validate TIFF, SIARD, PDF/A-Files and Submission 
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

package ch.kostceco.tools.kostval.validation.modulesip1.impl;

import java.io.File;
import java.util.List;

import ch.kostceco.tools.kostval.exception.modulesip1.Validation1bFolderStructureException;
import ch.kostceco.tools.kostval.validation.ValidationModuleImpl;
import ch.kostceco.tools.kostval.validation.modulesip1.Validation1bFolderStructureModule;
import ch.enterag.utils.zip.FileEntry;
import ch.enterag.utils.zip.Zip64File;

/**
 * Besteht eine korrekte prim�re Verzeichnisstruktur: /header/metadata.xml
 * /header/xsd /content
 * 
 * @author razm Daniel Ludin, Bedag AG @version 0.2.0
 */
public class Validation1bFolderStructureModuleImpl extends ValidationModuleImpl
		implements Validation1bFolderStructureModule
{

	@Override
	public boolean validate( File valDatei, File directoryOfLogfile )
			throws Validation1bFolderStructureException
	{

		boolean bExistsXsdFolder = false;
		boolean bExistsContentFolder = false;
		boolean bExistsMetadataFile = false;

		String toplevelDir = valDatei.getName();
		int lastDotIdx = toplevelDir.lastIndexOf( "." );
		toplevelDir = toplevelDir.substring( 0, lastDotIdx );

		try {

			Zip64File zipfile = new Zip64File( valDatei );
			List<FileEntry> fileEntryList = zipfile.getListFileEntries();
			for ( FileEntry fileEntry : fileEntryList ) {

				String name = fileEntry.getName();

				if ( (name.equals( "content/" ) || name.equals( toplevelDir
						+ "/content/" ))
						&& (fileEntry.isDirectory()) ) {
					bExistsContentFolder = true;
				}

				if ( (name.equals( "header/xsd/" ) || name.equals( toplevelDir
						+ "/header/xsd/" ))
						&& (fileEntry.isDirectory()) ) {
					bExistsXsdFolder = true;
				}

				if ( (name.equals( "header/metadata.xml" ) || name
						.equals( toplevelDir + "/header/metadata.xml" ))
						&& (!fileEntry.isDirectory()) ) {
					bExistsMetadataFile = true;
				}

			}
			zipfile.close();

		} catch ( Exception e ) {
			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_Aa_SIP )
							+ getTextResourceService().getText(
									ERROR_XML_UNKNOWN, e.getMessage() ) );

			return false;

		}
		if ( bExistsContentFolder == false ) {

			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_Ab_SIP )
							+ getTextResourceService().getText(
									ERROR_XML_AB_CONTENT ) );

		}
		if ( bExistsMetadataFile == false ) {

			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_Ab_SIP )
							+ getTextResourceService().getText(
									ERROR_XML_AB_METADATA ) );

		}
		if ( bExistsXsdFolder == false ) {

			getMessageService().logError(
					getTextResourceService().getText( MESSAGE_XML_MODUL_Ab_SIP )
							+ getTextResourceService().getText(
									ERROR_XML_AB_XSD ) );

		}

		return (bExistsContentFolder && bExistsMetadataFile && bExistsXsdFolder);
	}

}
