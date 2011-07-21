/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;

import junit.framework.TestCase;

import org.junit.Test;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TurnEdge;


public class TestOpenStreetMapGraphBuilder extends TestCase {

    @Test
    public void testGraphBuilder() throws Exception {

        Graph gg = new Graph();

        OpenStreetMapGraphBuilderImpl loader = new OpenStreetMapGraphBuilderImpl();
        FileBasedOpenStreetMapProviderImpl provider = new FileBasedOpenStreetMapProviderImpl();

        File file = new File(getClass().getResource("map.osm.gz").getFile());

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(gg);

        Vertex v2 = gg.getVertex("way 25660216 from 1"); //Kamiennogorska
        Vertex v2back = gg.getVertex("way 25660216 from 1 back"); //Kamiennogorska back
        Vertex v3 = gg.getVertex("way 25691274 from 0"); //Mariana Smoluchowskiego, right from Kamiennogorska
        Vertex v3back = gg.getVertex("way 25691274 from 0 back"); //ditto back
        Vertex v4 = gg.getVertex("way 25691274 from 3"); //Mariana Smoluchowskiego, left from Kamiennogorska
        Vertex v4back = gg.getVertex("way 25691274 from 3 back"); //ditto back
        assertNotNull(v2);
        assertNotNull(v2back);
        assertNotNull(v3);
        assertNotNull(v3back);
        assertNotNull(v4);
        assertNotNull(v4back);
        
        assertTrue ("name of v2 must be like \"Kamiennog\u00F3rska\"; was " + v2.getName(), v2.getName().contains("Kamiennog\u00F3rska"));
        assertTrue ("name of v3 must be like \"Mariana Smoluchowskiego\"; was " + v3.getName(), v3.getName().contains("Mariana Smoluchowskiego"));
        
        boolean v3EdgeExists = false;
        boolean v4EdgeExists = false;
        boolean v4BackEdgeExists = false;
        for (Edge e : gg.getOutgoing(v2)) {
            if (e instanceof TurnEdge) {
                TurnEdge t = (TurnEdge) e;
                Vertex tov = t.getToVertex();
                if (tov == v3 || tov == v3back) {
                    assertTrue("Turn cost wrong; expected ~90, was: " + t.turnCost, Math.abs(t.turnCost - 90) < 3);
                    v3EdgeExists = true;
                }
            }
        }
        
        for (Edge e : gg.getOutgoing(v2back)) {
            if (e instanceof TurnEdge) {
                TurnEdge t = (TurnEdge) e;
                Vertex tov = t.getToVertex();
                if (tov == v3 || tov == v3back) {
                    assertTrue(Math.abs(t.turnCost - 90) < 5);
                    v3EdgeExists = true;
                }
            }
        }
        
        for (Edge e : gg.getOutgoing(v3)) {
            if (e instanceof TurnEdge) {
                TurnEdge t = (TurnEdge) e;
                Vertex tov = t.getToVertex();
                if (tov == v4) {
                    assertTrue("Turn cost too big: " + t.turnCost, t.turnCost < 5);
                    v4EdgeExists = true;
                }
            }
        }
        
        for (Edge e : gg.getOutgoing(v4back)) {
            if (e instanceof TurnEdge) {
                TurnEdge t = (TurnEdge) e;
                Vertex tov = t.getToVertex();
                if (tov == v3back) {
                    assertTrue("Turn cost too big: " + t.turnCost, t.turnCost < 5);
                    v4BackEdgeExists = true;
                }
            }
        }

        assertTrue("There is no edge from v2 to v3", v3EdgeExists);
        assertTrue("There is no edge from v3 to v4", v4EdgeExists);
        assertTrue("There is no edge from v4back to v3back", v4BackEdgeExists);
    }
    
    public void testWayDataSet() {
    	OSMWay way = new OSMWay();
    	way.addTag("highway", "footway");
    	way.addTag("surface", "gravel");
    	way.addTag("access", "no");
    	
    	WayPropertySet wayPropertySet = new WayPropertySet();
    	
    	// where there are no way specifiers, the default is used
    	assertEquals(wayPropertySet.getDataForWay(way), wayPropertySet.defaultProperties);
    	
    	// add two equal matches: gravel only...
    	OSMSpecifier gravel_only = new OSMSpecifier();
    	gravel_only.addTag("surface","gravel");
    	
    	WayProperties gravel_is_safer = new WayProperties();
    	gravel_is_safer.setSafetyFeatures(new P2<Double>(1.5, 1.5));
    	
    	wayPropertySet.addProperties(gravel_only, gravel_is_safer);
    	
    	//and footway only
    	OSMSpecifier footway_only = new OSMSpecifier();
    	footway_only.addTag("highway","footway");
    	
    	WayProperties footways_allow_peds = new WayProperties();
    	footways_allow_peds.setPermission(StreetTraversalPermission.PEDESTRIAN);
    	
    	wayPropertySet.addProperties(footway_only, footways_allow_peds);
 
    	WayProperties dataForWay = wayPropertySet.getDataForWay(way);
    	//the first one is found
    	assertEquals(dataForWay, gravel_is_safer);
    	
    	//add a better match
    	OSMSpecifier gravel_and_footway = new OSMSpecifier();
    	gravel_and_footway.addTag("surface","gravel");
    	gravel_and_footway.addTag("highway","footway");
    	
    	WayProperties safer_and_peds = new WayProperties();
    	safer_and_peds.setSafetyFeatures(new P2<Double>(1.5, 1.5));
    	safer_and_peds.setPermission(StreetTraversalPermission.PEDESTRIAN);
    	
    	wayPropertySet.addProperties(gravel_and_footway, safer_and_peds);
    	dataForWay = wayPropertySet.getDataForWay(way);
    	assertEquals(dataForWay, safer_and_peds);
    }
    
    public void testCreativeNaming() {
    	OSMWay way = new OSMWay();
    	way.addTag("highway", "footway");
    	way.addTag("surface", "gravel");
    	way.addTag("access", "no");
    	
    	CreativeNamer namer = new CreativeNamer();
    	namer.setCreativeNamePattern("Highway with surface {surface} and access {access} and morx {morx}");
    	assertEquals("Highway with surface gravel and access no and morx ", namer.generateCreativeName(way));
    }
}
