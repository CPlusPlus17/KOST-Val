/*== KOST-Val ==================================================================================
The KOST-Val application is used for validate TIFF, SIARD, and PDF/A-Files. 
Copyright (C) 2012-2013 Claire R�thlisberger (KOST-CECO), Christian Eugster, Olivier Debenath, 
Peter Schneider (Staatsarchiv Aargau)
-----------------------------------------------------------------------------------------------
KOST-Val is a development of the KOST-CECO. All rights rest with the KOST-CECO. 
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

package ch.kostceco.tools.kostval.validation.moduletiff2;

import java.io.File;

import ch.kostceco.tools.kostval.exception.moduletiff2.ValidationBjhoveValidationException;
import ch.kostceco.tools.kostval.validation.ValidationModule;

/**
 * Validierungsschritt 3c (einschaltbar) Formatvalidierung, mit JHOVE oder einer
 * �hnlichen L�sung sowie mit einem externem PDF/A-Validator (z.B. PDF/A Manager
 * von PDFTRON) aller nach Dateiformat ausgew�hlten Dateien in /content
 * (konfigurierbare Liste von den zu validierenden Dateiformaten).
 * Referenzierung in Log-Datei zu den allf�lligen zus�tzlich generierten
 * Logdateien der eingesetzten Programme.
 * 
 * @author Rc Claire R�thlisberger, KOST-CECO
 */

public interface ValidationBjhoveValidationModule extends ValidationModule
{

	public boolean validate( File valDatei, File directoryOfLogfile )
			throws ValidationBjhoveValidationException;

}
