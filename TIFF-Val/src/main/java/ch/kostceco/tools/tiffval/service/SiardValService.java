/*== TIFF-Val ==================================================================================
The TIFF-Val application is used for validate Submission Information Package (SIP).
Copyright (C) 2013 Claire R�thlisberger (KOST-CECO)
-----------------------------------------------------------------------------------------------
TIFF-Val is a development of the KOST-CECO. All rights rest with the KOST-CECO. 
This application is free software: you can redistribute it and/or modify it under the 
terms of the GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version. 
 

This application is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the follow GNU General Public License for more details.
You should have received a copy of the GNU General Public License along with this program; 
if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
Boston, MA 02110-1301 USA or see <http://www.gnu.org/licenses/>.
==============================================================================================*/

package ch.kostceco.tools.tiffval.service;

import ch.kostceco.tools.tiffval.exception.SystemException;

/**
 * Service Interface f�r SIARD-Val.
 * 
 * @author Rc Claire R�thlisberger-Jourdan, KOST-CECO
 */
public interface SiardValService extends Service
{

	/**
	 * Gibt den Pfad zum SIARD-Val Executable zur�ck
	 * 
	 * @return Pfad zum SIARD-Val Executable
	 */
	public String getPathToSiardValJar();

	/**
	 * Setzt den Pfad zum SIARD-Val Executable
	 * 
	 * @return Pfad zum SIARD-Val Executable
	 */
	public void setPathToSiardValJar( String pathToSiardValJar );

	/**
	 * Gibt den Pfad zum Input File (das zu validierende Dokument) zur�ck
	 * 
	 * @return Pfad zum Input File
	 */
	public String getPathToInputFile();

	/**
	 * Setzt den Pfad zum Input File (dem zu validierenden Dokument)
	 * 
	 * @return Pfad zum Input File
	 */
	public void setPathToInputFile( String pathToInputFile );

	/**
	 * F�hrt die Validierung mit SIARD-Val aus
	 * 
	 * @return Pfad zum von SIARD-Val generierten LOG-Report
	 * @throws Exception
	 */
	public String executeSiardVal( String pathToSiardValJar,
			String pathToInputFile, String pathToOutput, String nameOfSip )
			throws SystemException;
}
