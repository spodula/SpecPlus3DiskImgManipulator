package dialogs;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

import diskviewer.libs.Speccy;
import diskviewer.libs.disk.cpm.CPM;

public class screenDialog extends DiskReaderDialog {
	// Filename of the source file
	public String filename = "";

	// Filename of the file on the disk.
	public String NameOnDisk = "";

	//Raw image from disk., 
	private BufferedImage RawImage = null;

	// black and white flag.
	private boolean isBW = false;

	// Raw scaled and converted image to be saved to the disk.
	public byte[] Screen = new byte[6912];

	// if TRUE, the OK button was pressed, else FALSE
	private boolean IsOk = false;

	// basic constructor
	public screenDialog(Shell parent) {
		super(parent);
	}

	/**
	 * Create and display the dialog.
	 * 
	 * @return
	 */
	public boolean open() {
		IsOk = false;
		Shell parent = getParent();
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setSize(800, 500);
		dialog.setText("Add an image file");
		dialog.setLayout(new GridLayout(4, false));

		Composite cp = new Composite(dialog, SWT.NONE);
		cp.setBackground(new Color(dialog.getDisplay(),0x80, 0x80, 0x80));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.horizontalSpan = 5;
		cp.setLayoutData(data);
		cp.setLayout(new GridLayout(1, false));

		Label label = GetLabel(cp, "Add an image file. (GIF, JPG, PNG supported)", 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);

		Button BtnSelFile = new Button(dialog, SWT.BORDER);
		data = new GridData();
		data.horizontalSpan = 1;
		data.verticalSpan = 6;
		data.minimumHeight = 192;
		data.minimumWidth = 256;
		BtnSelFile.setText("");
		BtnSelFile.setLayoutData(data);
		Image image = new Image(dialog.getDisplay(), 256, 192);
		BtnSelFile.setImage(image);

		GetLabel(dialog, "Source file", 3);
		Text FileNameEdit = GetText(dialog, "", 3);
		GetLabel(dialog, "Name on disk", 3);
		Text NameOnDiskEdit = GetText(dialog, "", 3);
		GetLabel(dialog, "Black/white?", 1);
		Button BtnIsBW = GetCheckbox(dialog, "", 1);
		GetLabel(dialog, "", 1);
		GetLabel(dialog, "B/W Luma", 1);
		Slider slider = new Slider(dialog, SWT.HORIZONTAL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		slider.setLayoutData(data);
		slider.setSelection(50);
		GetLabel(dialog, "", 1);

		slider.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				// by default goes from 0-100
				if (isBW) {
					int level = (int) (slider.getSelection() * 2.5);
					if (!FileNameEdit.getText().isEmpty()) {
						BtnSelFile.setImage(ScaleImage(dialog.getDisplay(), level));
					}
				}

			}
		});

		BtnIsBW.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				isBW = BtnIsBW.getSelection();
				if (!FileNameEdit.getText().isEmpty()) {
					BtnSelFile.setImage(ScaleImage(dialog.getDisplay(), 192));
				}
			}
		});

		BtnSelFile.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				FileDialog fd = new FileDialog(dialog, SWT.OPEN);
				fd.setText("Select file to add");
				fd.setFilterExtensions(new String[] { "*.*" });
				fd.setFileName("");
				String selected = fd.open();
				if (!selected.isBlank()) {
					FileNameEdit.setText(selected);
					File f = new File(selected);

					String fname = f.getName().toUpperCase();
					String filename = "";
					String extension = "";
					if (fname.contains(".")) {
						int i = fname.lastIndexOf(".");
						extension = fname.substring(i + 1);
						filename = fname.substring(0, i);
					} else {
						filename = fname;
					}
					filename = filename + "        ";
					filename = CPM.FixFilePart(filename.substring(0, 8).trim());

					extension = extension + "   ";
					extension = CPM.FixFilePart(extension.substring(0, 3).trim());
					// NameOnDisk
					NameOnDiskEdit.setText(filename + "." + extension);

					try {
						RawImage = ImageIO.read(new File(selected));

						BtnSelFile.setImage(ScaleImage(dialog.getDisplay(), 192));
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			}
		});

		Button BtnOK = GetButton(dialog, "Add", GridData.FILL);
		BtnOK.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				filename = FileNameEdit.getText();
				NameOnDisk = NameOnDiskEdit.getText();
				IsOk = true;
				dialog.dispose();
			}
		});

		Button btnCancel = GetButton(dialog, "Cancel", GridData.FILL);
		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				filename = "";
				NameOnDisk = "";
				IsOk = false;
				dialog.dispose();
			}
		});

		dialog.open();
		Display display = parent.getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		return (IsOk);
	}

	/**
	 * Scale the loaded image to 256x192 into a new image, run the speccy display conversion, 
	 * then return it as a SWT compatable image.  
	 * 
	 * @param selected
	 * @return
	 */
	public Image ScaleImage(Display display, int bwSlider) {
		BufferedImage result = new BufferedImage(256, 192, BufferedImage.TYPE_INT_RGB);
		Image SelectedImage=null;
		if (RawImage!=null) {
			// Draw the loaded image to the new buffer
			Graphics2D graphics2D = result.createGraphics();
			graphics2D.drawImage(RawImage, 0, 0, 256, 192, null);
			graphics2D.dispose();
			//process it. 
			if (isBW) {
				RenderBW(result,bwSlider);
			} else {
				RenderColour(result);
			}

			// render to output image
			graphics2D = result.createGraphics();
			graphics2D.dispose();
			ImageData id = convertToSWT(result);

			SelectedImage = new Image(display, id);
		} 


		return SelectedImage;
	}

	/**
	 * Render the currently loaded image into a Spectrum compatable bufferedimage.
	 * 
	 * @param result
	 */
	private void RenderColour(BufferedImage result) {
		// scale to 8 colours
		for (int x = 0; x < 256; x++) {
			for (int y = 0; y < 192; y++) {
				int col = result.getRGB(x, y);
				int red = (col & 0xff0000) >> 16;
				int green = (col & 0xff00) >> 8;
				int blue = col & 0xff;

				if ((red & 0x80) == 0x80) {
					red = 0xff;
				} else {
					red = 0x00;
				}
				if ((green & 0x80) == 0x80) {
					green = 0xff;
				} else {
					green = 0x00;
				}
				if ((blue & 0x80) == 0x80) {
					blue = 0xff;
				} else {
					blue = 0x00;
				}

				col = (red << 16) + (green << 8) + blue;
				result.setRGB(x, y, col);
			}
		}
		// Group into attributes
		int attriblocation = 0x1800;
		int colours[] = new int[8];
		for (int y = 0; y < 24; y++) {
			for (int x = 0; x < 32; x++) {
				// Blank the colour indexes
				for (int i = 0; i < 8; i++) {
					colours[i] = 0;
				}
				// base positions.
				int basex = x * 8;
				int basey = y * 8;
				// get the square
				for (int a = 0; a < 7; a++) {
					for (int b = 0; b < 7; b++) {
						// col = 00000000 RRRRRRRR GGGGGGGG BBBBBBBB
						// Speccy = 00000GRB
						int col = result.getRGB(basex + a, basey + b);
						int red = (col >> 16) & 0x02;
						int green = (col >> 8) & 0x04;
						int blue = (col & 0x01);
						col = red + green + blue;
						colours[col]++;
					}
				}
				// find the max and max-1
				int ink = 0;
				int paper = 0;

				int maxnum = 0;
				for (int i = 0; i < 8; i++) {
					if (colours[i] > maxnum) {
						ink = i;
						maxnum = colours[i];
					}
				}

				colours[ink] = 0;
				maxnum = 0;
				for (int i = 0; i < 8; i++) {
					if (colours[i] > maxnum) {
						paper = i;
						maxnum = colours[i];
					}
				}
				if (maxnum == 0) {
					paper = ink;
				}
				// make an array of colours
				int newcolours[] = new int[8];
				for (int i = 0; i < 8; i++) {
					newcolours[i] = Speccy.colours[ink];
				}
				newcolours[paper] = Speccy.colours[paper];

				// rewrite the square
				Screen[attriblocation++] = (byte) (ink + (paper * 8));
				for (int a = 0; a < 8; a++) {
					int byt = 0;
					for (int b = 0; b < 8; b++) {
						int col = result.getRGB(basex + b, basey + a);
						int red = (col >> 16) & 0x02;
						int green = (col >> 8) & 0x04;
						int blue = (col & 0x01);
						col = red + green + blue;

						int newcol = newcolours[col];

						result.setRGB(basex + b, basey + a, newcol);
						// calculate if we are ink or paper.
						byt = byt << 1;
						if (newcol == Speccy.colours[ink]) {
							byt = byt + 1;
						}
					}
					// calculate the pixel data location [ 000 aabbb cccxxxxx ] where yptn =
					// [aacccbbb]
					int yptn = basey + a;
					int y1 = yptn & 0x07;
					int y2 = (yptn & 0x38) >> 3;
					int y3 = (yptn & 0xc0) >> 6;
					int address = (y3 << 11) + (y1 << 8) + (y2 << 5) + (basex >> 3);
					// write the pixel data
					Screen[address] = (byte) (byt & 0xff);

				}
			}
		}
		
	}

	/**
	 * Render the currently loaded images as a black and white image.
	 * Note RGB->lum values are from ITU BT.601.
	 * 
	 * @param result
	 * @param bwSlider
	 */
	private void RenderBW(BufferedImage result,int bwSlider) {
		//store for pixel data
		boolean pixels[] = new boolean[49152];
		int pxIdx = 0;
		
		//loop every pixel.
		for (int y = 0; y < 192; y++) {
			for (int x = 0; x < 256; x++) {
				//get the RGB values
				int col = result.getRGB(x, y);
				int red = (col & 0xff0000) >> 16;
				int green = (col & 0xff00) >> 8;
				int blue = col & 0xff;

				//convert into a luminance (Greyscale) value.
				double lum = (0.299 * red) + (0.587 * green) + (0.114 * blue);
				int iLum = (int) Math.round(lum);

				//See if the Luminance crosses the value, if so set the local image. 
				if (iLum > bwSlider) {
					iLum = 0xffffff;
					pixels[pxIdx++] = false;
				} else {
					iLum = 0x00;
					pixels[pxIdx++] = true;
				}

				result.setRGB(x, y, iLum);
			}
		}

		pxIdx = 0;
		for (int y = 0; y < 192; y++) {
			// calculate the pixel data location [ 000 aabbb cccxxxxx ] where yptn =
			// [aacccbbb]
			int y1 = y & 0x07;
			int y2 = (y & 0x38) >> 3;
			int y3 = (y & 0xc0) >> 6;
			int baseYAddress = (y3 << 11) + (y1 << 8) + (y2 << 5);

			// write the line
			for (int x = 0; x < 32; x++) {
				int byt = 0;
				for (int b = 0; b < 8; b++) {
					boolean px = pixels[pxIdx++];
					int col = 0;
					if (px) {
						col = 1;
					}
					byt = (byt << 1) + col;
				}
				int address = baseYAddress + x;
				// write the pixel data
				Screen[address] = (byte) (byt & 0xff);
			}
		}
		// make the entire attribute area black on white.
		for (int i = 0x1800; i < 0x1b00; i++) {
			Screen[i] = 0x38;
		}
		
		
	}

	/**
	 * snippet 156: convert between SWT Image and AWT BufferedImage.
	 * <p>
	 * For a list of all SWT example snippets see
	 * http://www.eclipse.org/swt/snippets/
	 */
	public static ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(),
					colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int rgb = bufferedImage.getRGB(x, y);
					int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
					data.setPixel(x, y, pixel);
					if (colorModel.hasAlpha()) {
						data.setAlpha(x, y, (rgb >> 24) & 0xFF);
					}
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
			ComponentColorModel colorModel = (ComponentColorModel) bufferedImage.getColorModel();
			// ASSUMES: 3 BYTE BGR IMAGE TYPE
			PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			// This is valid because we are using a 3-byte Data model with no transparent
			// pixels
			data.transparentPixel = -1;
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		}
		return null;
	}

}
