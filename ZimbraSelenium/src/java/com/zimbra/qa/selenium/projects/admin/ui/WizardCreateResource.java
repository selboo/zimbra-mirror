/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
/**
 * 
 */
package com.zimbra.qa.selenium.projects.admin.ui;

import java.awt.event.KeyEvent;

import com.zimbra.qa.selenium.framework.items.IItem;
import com.zimbra.qa.selenium.framework.ui.AbsTab;
import com.zimbra.qa.selenium.framework.ui.AbsWizard;
import com.zimbra.qa.selenium.framework.util.HarnessException;
import com.zimbra.qa.selenium.framework.util.SleepUtil;
import com.zimbra.qa.selenium.framework.util.ZimbraSeleniumProperties;
import com.zimbra.qa.selenium.projects.admin.items.ResourceItem;


/**
 * @author Matt Rhoades
 *
 */
public class WizardCreateResource extends AbsWizard {
	public static class Locators {
		public static final String zdlg_RESOURCE_NAME = "zdlgv__NEW_RES_displayName";
		public static final String zdlg_RESOURCE_LOCAL_NAME = "zdlgv__NEW_RES_name";
		public static final String zdlg_RESOURCE_DOMAIN_NAME="zdlgv__NEW_RES_name_2_display";
		public static final String zdlg_OK="zdlg__NEW_ALIAS_button2_title";
		public static final String zdlg_RESOURCE_TYPE="zdlgv__NEW_RES_zimbraCalResType_display";
		public static final String zdlg_RESOURCE_TYPE_LOCATION="zdlgv__NEW_RES_zimbraCalResType_choice_0";
		public static final String zdlg_RESOURCE_TYPE_EQUIPMENT="zdlgv__NEW_RES_zimbraCalResType_choice_1";
		public static final String LOCATION="Location";
		public static final String EQUIPMENT="Equipment";
	}
	public String resourceType="";


	public WizardCreateResource(AbsTab page) {
		super(page);
	}

	@Override
	public IItem zCompleteWizard(IItem item) throws HarnessException {

		if ( !(item instanceof ResourceItem) )
			throw new HarnessException("item must be an ResourceItem, was "+ item.getClass().getCanonicalName());

		ResourceItem resource = (ResourceItem)item;


		String CN = resource.getLocalName();
		String domain = resource.getDomainName();
		
		
		sType(Locators.zdlg_RESOURCE_NAME, CN);
		sType(Locators.zdlg_RESOURCE_LOCAL_NAME, CN);

		/**
		 * If you use normal type method domain is taken as default domain name.
		 * Below line of code is not grid friendly but this is only solution working currently. 
		 */
		if(ZimbraSeleniumProperties.isWebDriver()) {
			SleepUtil.sleepSmall();
			this.clearField(Locators.zdlg_RESOURCE_DOMAIN_NAME);	
		}
		zType(Locators.zdlg_RESOURCE_DOMAIN_NAME,"");
		zType(Locators.zdlg_RESOURCE_DOMAIN_NAME,domain);
		
		this.zKeyboard.zTypeKeyEvent(KeyEvent.VK_ENTER);

		
		if(resourceType!="") {
			sClick(Locators.zdlg_RESOURCE_TYPE);
			if(resourceType.equals(Locators.LOCATION)) {
				sClick(Locators.zdlg_RESOURCE_TYPE_LOCATION);
			} else if(resourceType.equals(Locators.EQUIPMENT)) {
				sClick(Locators.zdlg_RESOURCE_TYPE_EQUIPMENT);
			}
		}

		clickFinish(AbsWizard.Locators.RESOURCE_DIALOG);

		return resource;
	}

	@Override
	public String myPageName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean zIsActive() throws HarnessException {
		// TODO Auto-generated method stub
		return false;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}


}
