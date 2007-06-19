//=============================================================================
//===	Copyright (C) 2001-2007 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.kernel.harvest.harvester.geonet;

import java.util.ArrayList;
import java.util.Iterator;
import jeeves.exceptions.BadInputEx;
import jeeves.exceptions.BadParameterEx;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.harvest.harvester.AbstractParams;
import org.fao.geonet.lib.Lib;
import org.jdom.Element;

//=============================================================================

public class GeonetParams extends AbstractParams
{
	//--------------------------------------------------------------------------
	//---
	//--- Constructor
	//---
	//--------------------------------------------------------------------------

	public GeonetParams(DataManager dm)
	{
		super(dm);
	}

	//---------------------------------------------------------------------------
	//---
	//--- Create : called when a new entry must be added. Reads values from the
	//---          provided entry, providing default values
	//---
	//---------------------------------------------------------------------------

	public void create(Element node) throws BadInputEx
	{
		super.create(node);

		Element site     = node.getChild("site");
		Element opt      = node.getChild("options");
		Element searches = node.getChild("searches");

		host    = getValue(site, "host",    "");
		port    = getValue(site, "port",    80);
		servlet = getValue(site, "servlet", "geonetwork");

		createGroups = getValue(opt, "createGroups", true );
		createCateg  = getValue(opt, "createCateg",  true );

		checkPort(port);
		addSearches(searches);
	}

	//---------------------------------------------------------------------------
	//---
	//--- Update : called when an entry has changed and variables must be updated
	//---
	//---------------------------------------------------------------------------

	public void update(Element node) throws BadInputEx
	{
		super.update(node);

		Element site     = node.getChild("site");
		Element opt      = node.getChild("options");
		Element searches = node.getChild("searches");

		host    = getValue(site, "host",    host);
		port    = getValue(site, "port",    port);
		servlet = getValue(site, "servlet", servlet);

		createGroups = getValue(opt, "createGroups", createGroups);
		createCateg  = getValue(opt, "createCateg",  createCateg);

		checkPort(port);

		//--- if some search queries are given, we drop the previous ones and
		//--- set these new ones

		if (searches != null)
			addSearches(searches);
	}

	//---------------------------------------------------------------------------
	//---
	//--- Other API methods
	//---
	//---------------------------------------------------------------------------

	public Iterable<Search> getSearches() { return alSearches; }

	//---------------------------------------------------------------------------

	public GeonetParams copy()
	{
		GeonetParams copy = new GeonetParams(dm);
		copyTo(copy);

		copy.host    = host;
		copy.port    = port;
		copy.servlet = servlet;

		copy.createGroups = createGroups;
		copy.createCateg  = createCateg;

		for (Search s : alSearches)
			copy.alSearches.add(s.copy());

		return copy;
	}

	//---------------------------------------------------------------------------
	//---
	//--- Private methods
	//---
	//---------------------------------------------------------------------------

	private void addSearches(Element searches) throws BadInputEx
	{
		alSearches.clear();

		if (searches == null)
			return;

		Iterator searchList = searches.getChildren("search").iterator();

		while (searchList.hasNext())
		{
			Element search = (Element) searchList.next();

			Search s = new Search();

			s.freeText = getValue(search, "freeText", "");
			s.title    = getValue(search, "title",    "");
			s.abstrac  = getValue(search, "abstract", "");
			s.keywords = getValue(search, "keywords", "");
			s.digital  = getValue(search, "digital",  false);
			s.hardcopy = getValue(search, "hardcopy", false);
			s.siteId   = getValue(search, "siteId",   "");

			alSearches.add(s);

			if (s.siteId.equals(""))
				throw new BadParameterEx("siteId", "");
		}
	}

	//---------------------------------------------------------------------------
	//---
	//--- Variables
	//---
	//---------------------------------------------------------------------------

	public String  host;
	public int     port;
	public String  servlet;

	public boolean createGroups;
	public boolean createCateg;

	private ArrayList<Search> alSearches = new ArrayList<Search>();
}

//=============================================================================

class Search
{
	//---------------------------------------------------------------------------
	//---
	//--- API methods
	//---
	//---------------------------------------------------------------------------

	public Search copy()
	{
		Search s = new Search();

		s.freeText = freeText;
		s.title    = title;
		s.abstrac  = abstrac;
		s.keywords = keywords;
		s.digital  = digital;
		s.hardcopy = hardcopy;
		s.siteId   = siteId;

		return s;
	}

	//---------------------------------------------------------------------------

	public Element createRequest()
	{
		Element req = new Element("request");

		Lib.element.add(req, "any",      freeText);
		Lib.element.add(req, "title",    title);
		Lib.element.add(req, "abstract", abstrac);
		Lib.element.add(req, "themekey", keywords);
		Lib.element.add(req, "siteId",   siteId);

		if (digital)
			Lib.element.add(req, "digital", "on");

		if (hardcopy)
			Lib.element.add(req, "paper", "on");

		return req;
	}

	//---------------------------------------------------------------------------
	//---
	//--- Variables
	//---
	//---------------------------------------------------------------------------

	public String  freeText;
	public String  title;
	public String  abstrac;
	public String  keywords;
	public boolean digital;
	public boolean hardcopy;
	public String  siteId;
}

//=============================================================================

