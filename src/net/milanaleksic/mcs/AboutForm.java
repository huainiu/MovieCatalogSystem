package net.milanaleksic.mcs;

import java.awt.Desktop;
import java.net.URI;

import net.milanaleksic.mcs.util.Kernel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.graphics.Font;


public class AboutForm {
	
	private static final String dodatniTekst = 
		"Верзија програма: " + Kernel.getVersion() + "\n\n" +
		"У развоју су коришћене следеће бесплатне технологије:\n"+
		"Јава Мустанг, Eclipse SWT, Spring 2, Hibernate 3, \n"+
		"IBM DB2 Express 9.5, C3P0 итд.\n\n"+
		"Иконе су део \"Crystal Project\"-а, аутор је Евералдо Келхо.\n\n"+
		"Програм је још увек у развоју, све грешке молим пријавите аутору програма"
		;  //  @jve:decl-index=0:

	private Shell sShell = null;  //  @jve:decl-index=0:visual-constraint="10,10"
	private Shell parent = null; // @jve:decl-index=0:
	private Text textArea = null;
	private Composite composite = null;
	private Label labEmail = null;
	private Button btnEmail = null;
	private Composite composite1 = null;
	private Label labSite = null;
	private Button btnSite = null;
	private Composite composite2 = null;
	private Label label = null;
	private Label label1 = null;
	private Label label2 = null;

	private Label label3 = null;
	
	public AboutForm(Shell parent) {
		this.parent = parent;
		createSShell();
		sShell.setLocation(
				new Point(
						parent.getLocation().x+Math.abs(parent.getSize().x-sShell.getSize().x) / 2, 
						parent.getLocation().y+Math.abs(parent.getSize().y-sShell.getSize().y) / 2 ));
		textArea.setText(textArea.getText() + "\n\n" + AboutForm.dodatniTekst);
		sShell.open();
	}
	
	/**
	 * This method initializes sShell
	 */
	private void createSShell() { 
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		gridLayout.verticalSpacing = 10;
		gridLayout.makeColumnsEqualWidth = true;
		GridData gridData1 = new GridData();
		gridData1.grabExcessHorizontalSpace = true;
		gridData1.grabExcessVerticalSpace = true;
		gridData1.verticalAlignment = org.eclipse.swt.layout.GridData.FILL;
		gridData1.heightHint = -1;
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.FILL;
		if (parent == null)
			sShell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		else
			sShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		sShell.setText("О програму");
		sShell.setLayout(gridLayout);
		sShell.setSize(new Point(412, 326));
		createComposite2();
		createComposite();
		createComposite1();
		sShell.addShellListener(new org.eclipse.swt.events.ShellAdapter() {
			public void shellClosed(org.eclipse.swt.events.ShellEvent e) {
				sShell.dispose();
			}
		});
		textArea = new Text(sShell, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY | SWT.CENTER | SWT.BORDER);
		textArea.setText("Copyright(C)2007 by Milan Aleksic");
		textArea.setLayoutData(gridData1);
	}

	/**
	 * This method initializes composite	
	 *
	 */
	private void createComposite() {
		GridData gridData3 = new GridData();
		gridData3.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 2;
		composite = new Composite(sShell, SWT.BORDER);
		composite.setLayout(gridLayout1);
		composite.setLayoutData(gridData3);
		labEmail = new Label(composite, SWT.NONE);
		labEmail.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
		labEmail.setText("milan.aleksic@gmail.com");
		btnEmail = new Button(composite, SWT.NONE);
		btnEmail.setText("пошаљи email");
		btnEmail.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				new Thread(new Runnable() {

					public void run() {
						if (Desktop.isDesktopSupported()) {
							Desktop desktop = Desktop.getDesktop();
							if (desktop.isSupported(Desktop.Action.MAIL)) {
								try {
									desktop.mail(new URI("mailto:milan.aleksic@gmail.com"));
								}
								catch (Exception exc) {
									exc.printStackTrace();
								}
							}
						}
					}
					
				}).start();
			}
		});
	}

	/**
	 * This method initializes composite1	
	 *
	 */
	private void createComposite1() {
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridLayout gridLayout2 = new GridLayout();
		gridLayout2.numColumns = 2;
		composite1 = new Composite(sShell, SWT.BORDER);
		composite1.setLayout(gridLayout2);
		composite1.setLayoutData(gridData2);
		labSite = new Label(composite1, SWT.NONE);
		labSite.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
		labSite.setText("http://galeb.etf.bg.ac.yu/~aleksicm/  ");
		btnSite = new Button(composite1, SWT.NONE);
		btnSite.setText("иди");
		btnSite.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				new Thread(new Runnable() {

					public void run() {
						if (Desktop.isDesktopSupported()) {
							Desktop desktop = Desktop.getDesktop();
							if (desktop.isSupported(Desktop.Action.BROWSE)) {
								try {
									desktop.browse(URI.create("http://galeb.etf.bg.ac.yu/~aleksicm"));
								}
								catch (Exception exc) {
									exc.printStackTrace();
								}
							}
						}
					}
					
				}).start();
			}
		});
	}

	/**
	 * This method initializes composite2	
	 *
	 */
	private void createComposite2() {
		GridData gridData11 = new GridData();
		gridData11.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData6 = new GridData();
		gridData6.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData5 = new GridData();
		gridData5.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData5.grabExcessHorizontalSpace = true;
		GridData gridData4 = new GridData();
		gridData4.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		GridLayout gridLayout3 = new GridLayout();
		gridLayout3.numColumns = 1;
		composite2 = new Composite(sShell, SWT.NONE);
		composite2.setLayout(gridLayout3);
		composite2.setLayoutData(gridData5);
		label3 = new Label(composite2, SWT.CENTER);
		label3.setText("Movie Catalog System");
		label3.setFont(new Font(Display.getDefault(), "Segoe UI", 12, SWT.BOLD));
		label3.setLayoutData(gridData11);
		label3.setForeground(new Color(Display.getCurrent(), 0, 0, 255));
		label = new Label(composite2, SWT.NONE);
		label.setText("Аутор програма је Милан Алексић");
		label.setFont(new Font(Display.getDefault(), "Segoe UI", 10, SWT.BOLD));
		label.setLayoutData(gridData);
		label1 = new Label(composite2, SWT.NONE);
		label1.setText("апсолвент Електротехничког Факултета у Београду");
		label1.setFont(new Font(Display.getDefault(), "Segoe UI", 10, SWT.BOLD));
		label1.setLayoutData(gridData4);
		label2 = new Label(composite2, SWT.NONE);
		label2.setText("новембар 2007 - јануар 2008");
		label2.setLayoutData(gridData6);
	}

}
