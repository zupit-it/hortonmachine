/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.modules;
import static org.jgrasstools.gears.libs.modules.JGTConstants.GEOMORPHOLOGY;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.hortonmachine.modules.geomorphology.geomorphon.OmsGeomorphon;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(OmsGeomorphon.DESCRIPTION)
@Author(name = OmsGeomorphon.AUTHORS, contact = OmsGeomorphon.CONTACT)
@Keywords(OmsGeomorphon.KEYWORDS)
@Label(GEOMORPHOLOGY)
@Name(OmsGeomorphon.NAME)
@Status(OmsGeomorphon.STATUS)
@License(OmsGeomorphon.LICENSE)
public class Geomorphon extends OmsGeomorphon {
    @Description(inELEV_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev;

    @Description(pRadius_DESCR)
    @Unit(pRadius_UNIT)
    @In
    public double pRadius;

    @Description(pThreshold_DESCR)
    @Unit(pThreshold_UNIT)
    @In
    public double pThreshold = 1;

    @Description(outRaster_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        OmsGeomorphon geomorphon = new OmsGeomorphon();
        geomorphon.inElev = getRaster(inElev);
        geomorphon.pRadius = pRadius;
        geomorphon.pThreshold = pThreshold;
        geomorphon.pm = pm;
        geomorphon.process();
        dumpRaster(geomorphon.outRaster, outRaster);
    }

}
