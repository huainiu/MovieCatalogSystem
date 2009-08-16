/**
 * 
 */
package net.milanaleksic.mcs;

import net.milanaleksic.mcs.db.Film;

import org.apache.log4j.Logger;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Query;
import org.hibernate.Transaction;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;


import org.eclipse.swt.widgets.Canvas;

/**
 * @author Milan Aleksic 09.12.2007.
 */
public class DeleteMovieForm {

	private static Logger logger = Logger.getLogger(DeleteMovieForm.class);  //  @jve:decl-index=0:

	private Shell sShell = null; // @jve:decl-index=0:visual-constraint="10,10"
	private Label labUpozorenje = null;
	private Shell parent = null;
	private Integer filmId = null;
	private Runnable parentRunner = null;
	private Composite composite = null;
	private Button btnOk = null;
	private Button btnCancel = null;
	private Label labFilmNaziv = null;
	private Canvas canvas = null;

	public DeleteMovieForm(Shell parent, Integer filmId, Runnable runnable) {
		this.parent = parent;
		createSShell();
		logger.info("DeleteMovieForm: FILMID=" + filmId);
		sShell.setLocation(new Point(parent.getLocation().x + Math.abs(parent.getSize().x - sShell.getSize().x) / 2, parent.getLocation().y
				+ Math.abs(parent.getSize().y - sShell.getSize().y) / 2));
		this.filmId = filmId;
		reReadData();
		sShell.pack();
		sShell.open();
		parentRunner = runnable;
	}

	protected void reReadData() {
		final HibernateTemplate template = Startup.getKernel().getHibernateTemplate();
		template.execute(new HibernateCallback() {

			public Object doInHibernate(org.hibernate.Session session) throws org.hibernate.HibernateException, java.sql.SQLException {

				// preuzimanje podataka za film koji se azurira
				if (filmId != null) {
					Query query = session.createQuery("from Film f where f.idfilm = :id");
					query.setInteger("id", filmId.intValue());
					java.util.List<?> list = query.list();
					if (list.size() != 1) {
						MessageBox box = new MessageBox(sShell, SWT.ICON_ERROR);
						box.setMessage("Нисам успео да јединствено идентификујем филм у бази, добио сам "+ list.size() + " одговора");
						box.setText("Грешка");
						box.open();
						logger.error("Nisam uspeo da jedinstveno identifikujem film u bazi, " + "na upit sam dobio " + list.size() + " odgovora");
						return null;
					}
					Film film = (Film) list.get(0);
					labFilmNaziv.setText(film.getNazivfilma() + "\n(" + film.getPrevodnazivafilma() + ")");

				}
				return null;
			};

		});
	}

	/**
	 * This method initializes sShell
	 */
	private void createSShell() {
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.numColumns = 2;
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData2.grabExcessVerticalSpace = true;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData.grabExcessHorizontalSpace = true;
		if (parent == null)
			sShell = new Shell(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		else
			sShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		sShell.setText("Брисање филма");
		sShell.setLayout(gridLayout1);
		createCanvas();
		sShell.setSize(new Point(431, 154));
		labUpozorenje = new Label(sShell, SWT.NONE);
		labFilmNaziv = new Label(sShell, SWT.WRAP | SWT.SHADOW_OUT | SWT.HORIZONTAL | SWT.CENTER);
		labUpozorenje.setText("Да ли заиста желите да избришете филм??");
		labUpozorenje.setLayoutData(gridData);
		labUpozorenje.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
		labUpozorenje.setFont(new Font(Display.getDefault(), "Segoe UI", 12, SWT.BOLD));
		labFilmNaziv.setText("[ назив филма ]");
		labFilmNaziv.setLayoutData(gridData2); 
		labFilmNaziv.setFont(new Font(Display.getDefault(), "Segoe UI", 12, SWT.BOLD));
		new Label(sShell, SWT.NONE);
		createComposite();
		sShell.addShellListener(new org.eclipse.swt.events.ShellAdapter() {
			public void shellClosed(org.eclipse.swt.events.ShellEvent e) {
				sShell.dispose();
			}
		});
	}

	/**
	 * This method initializes composite
	 * 
	 */
	private void createComposite() {
		GridData gridData1 = new GridData();
		gridData1.horizontalAlignment = org.eclipse.swt.layout.GridData.CENTER;
		gridData1.grabExcessHorizontalSpace = true;
		GridData gridData12 = new GridData();
		gridData12.horizontalAlignment = GridData.END;
		gridData12.grabExcessVerticalSpace = true;
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.horizontalSpacing = 20;
		composite = new Composite(sShell, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(gridData1);
		btnOk = new Button(composite, SWT.NONE);
		btnOk.setText("ОБРИШИ!");
		btnCancel = new Button(composite, SWT.NONE);
		btnCancel.setText("\u041e\u0434\u0443\u0441\u0442\u0430\u043d\u0438");
		btnCancel.setLayoutData(gridData12);
		btnOk.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				final HibernateTemplate template = Startup.getKernel().getHibernateTemplate();
				template.execute(new HibernateCallback() {
					
					public Object doInHibernate(org.hibernate.Session session) throws org.hibernate.HibernateException ,java.sql.SQLException {
						
						// preuzimanje podataka za film koji se azurira
						if (filmId != null) {
							Transaction transaction = session.beginTransaction();
							Query query = session.createQuery("from Film f where f.idfilm = :id");
							query.setInteger("id", filmId.intValue());
							java.util.List<?> list = query.list();
							if (list.size() != 1) {
								logger.error("Nisam uspeo da jedinstveno identifikujem film u bazi, " +
										"na upit sam dobio " + list.size() + " odgovora");
								return null;
							}
							Film film = (Film) list.get(0);
							session.delete(film);
							transaction.commit();
						}
						return null;
					};
					
				});
				
				parentRunner.run();
				sShell.close();
			}
		});
		btnCancel.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				sShell.close();
			}
		});
	}

	/**
	 * This method initializes canvas
	 * 
	 */
	private void createCanvas() {
		GridData gridData3 = new GridData();
		gridData3.verticalSpan = 2;
		gridData3.heightHint = 64;
		gridData3.widthHint = 64;
		canvas = new Canvas(sShell, SWT.NONE);
		canvas.setLayoutData(gridData3);

		Color bckg = canvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
		canvas.setBackground(bckg);
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				GC gc = e.gc;
				Image buffer = new Image(Display.getCurrent(), getClass().getResourceAsStream("/net/milanaleksic/mcs/res/alert.png"));
				gc.drawImage(buffer, 0, 0);
				buffer.dispose();
			}
		});
	}

}
