package osmowsis;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class LawnUI {

    public Lawn lwn;

	protected int lawn_x, lawn_y;

	private JFrame frame;
	private JPanel panel1;
	private JPanel panel2;
	private JTable table;
	private JTextPane pane;

	public ImageIcon grass_icon;
	public ImageIcon crater_icon;
	public ImageIcon energy_icon;
	public ImageIcon mower_icon;
	public ImageIcon crash_icon;
	public ImageIcon charge_icon;
	public ImageIcon mower_icon1;

	static final int DEFAULT_PIXELS = 40;

	private int translate_lawn_y(int ypos) {
		return (lawn_y - 1 - ypos);
	}

	LawnUI(Lawn lawn) {

		Cell c;
		int x_pixels = lawn.width * DEFAULT_PIXELS; // default pixels per cell
		int y_pixels = lawn.height * DEFAULT_PIXELS; // default pixels per cell
        int x_pxl_width = (x_pixels < 400) ? 400 : x_pixels;

		lawn_x = lawn.width;
		lawn_y = lawn.height;

		lwn = lawn;

		frame = new JFrame("OSMOWSIS");

		frame.setSize(x_pxl_width+20, y_pixels + 350);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		panel1 = new JPanel(null);
		panel1.setLayout(new BorderLayout());
		panel1.setBackground(Color.BLACK);
		panel1.setSize(x_pxl_width+20, y_pixels + 350);
		frame.add(panel1);

		panel2 = new JPanel();
		panel2.setSize(x_pxl_width, y_pixels);
		panel2.setBorder(BorderFactory.createLineBorder(Color.WHITE));

		panel1.add(panel2);

		table = new JTable(lawn.height, lawn.width);
		table.setRowHeight(DEFAULT_PIXELS);
		table.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

		TableColumn tc;
		for (int i = 0; i < lawn.width; i++) {
			tc = table.getColumnModel().getColumn(i);
			tc.setPreferredWidth(DEFAULT_PIXELS);
		}
		panel2.add(table);

		setup_icons();

		;
		pane = new JTextPane();
		pane.setBounds(0, y_pixels , x_pixels, 300);

		pane.setBackground(new Color(235,254,252));
		pane.setBounds(0, y_pixels, x_pxl_width, 250);
		pane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		panel1.add(pane);

		JPanel panel3 = new JPanel(null);
		panel3.setBounds(0, y_pixels, x_pixels, 50);
		panel3.setBackground(new Color(44,142,229));

		Dimension d = new Dimension(100, 100);
		JButton button = new JButton("NEXT>");
		button.setPreferredSize(d);
		button.setBackground(Color.green);
		button.setBounds((x_pixels / 2) - 80, y_pixels + 250, 90, 40);
		panel3.add(button);


		button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
				    lwn.activate_next_action = 1;
				}
		});

		JButton button2 = new JButton("FAST>>");
		button2.setPreferredSize(d);
		button2.setBackground(new Color(41,129,14));
		button2.setBounds((x_pixels / 2) + 20, y_pixels + 250, 90, 40);
		panel3.add(button2);

		button2.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
				    lwn.button_mode = 1;
				    lwn.activate_next_action = 1;
				}
				});

		JButton button3 = new JButton("STOP");
		button3.setPreferredSize(d);
		button3.setBackground(Color.RED);
		button3.setBounds((x_pixels / 2) + 120, y_pixels + 250, 90, 40);
		panel3.add(button3);
		panel1.add(panel3);

		button3.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
                    lwn.stop = 1;
				}
				});


		panel3.setVisible(true);
		frame.setVisible(true);
		panel1.setVisible(true);
		panel2.setVisible(true);

		render_lawn(lawn);
	}

	public String lawn_pane_report(Lawn lawn) {
		String str = "";

		str += "Mower_ID, Active_Turn, Energy, Current_State\r\n";
		for (int i = 0; i < lawn.mowers.size(); i++) {
			Mower m = lawn.mowers.get(i);

			str += m.id + "                  ";
			str += (m.active_turn == 1) ? "YES              " : "no               ";
			str += m.energy_units + "            ";
			str += m.mower_state.get_state_str();
			str += "\r\n";
		}
		str += "\r\n";
		str += "grass_cut, total_grass, turns_remain\r\n";
		str += lawn.grass_cut + "                ";
		str += lawn.total_grass + "             ";
		str += lawn.turns_remain + "\r\n";

		if (lawn.terminated)
			str += "SIMULATION DONE\r\n";

		return str;
	}

	public void render_lawn(Lawn lawn) {
		Cell c;
		Mower m;

		for (int j = 0; j < lawn.height; j++) {
			for (int i = 0; i < lawn.width; i++) {
				c = lawn.scan_cell(i, j);
				int height = translate_lawn_y(j);

				table.getColumnModel().getColumn(i).setCellRenderer(new ImageRenderer());
				DefaultTableModel tmodel = (DefaultTableModel) table.getModel();

				if (c.state == LawnState.GRASS)
					tmodel.setValueAt(grass_icon, height, i);
				else if (c.state == LawnState.CRATER)
					tmodel.setValueAt(crater_icon, height, i);
				else if (c.state == LawnState.ENERGY)
					tmodel.setValueAt(energy_icon, height, i);
				else if (c.state == LawnState.EMPTY)
					tmodel.setValueAt(null, height, i);
				if (c.state == LawnState.MOWER) {
					m = lawn.get_mower(c.mower_id);

					//test
					String mow=Integer.toString(c.mower_id);
					mow_icon(mow);
					//
					if (m.mower_state == MowerState.CRASHED)
						tmodel.setValueAt(crash_icon, height, i);
					else if (c.recharge)
						tmodel.setValueAt(charge_icon, height, i);
					else {
						if (c.mower_id > 10) {
							tmodel.setValueAt(mower_icon, height, i);
						}
						else {
							tmodel.setValueAt(mower_icon1, height, i);
						}
					}
				}

				tmodel.fireTableDataChanged();
			}
		}
		pane.setText(lawn_pane_report(lawn));
	}

	// for testing

	private void mow_icon(String mow_id){
		ImageIcon tmp1 = new ImageIcon("./img/m_"+mow_id+".jpg");
		mower_icon1 = new ImageIcon(tmp1.getImage().getScaledInstance(DEFAULT_PIXELS, DEFAULT_PIXELS, Image.SCALE_DEFAULT));
	}


	private void setup_icons() {
		ImageIcon tmp;

		tmp = new ImageIcon("./img/grass3.jpg");
		grass_icon = new ImageIcon(tmp.getImage().getScaledInstance(DEFAULT_PIXELS, DEFAULT_PIXELS, Image.SCALE_DEFAULT));
		tmp = new ImageIcon("./img/Crater.jpg");
		crater_icon = new ImageIcon(tmp.getImage().getScaledInstance(DEFAULT_PIXELS, DEFAULT_PIXELS, Image.SCALE_DEFAULT));
		tmp = new ImageIcon("./img/energy1.jpg");
		energy_icon = new ImageIcon(tmp.getImage().getScaledInstance(DEFAULT_PIXELS, DEFAULT_PIXELS, Image.SCALE_DEFAULT));
		tmp = new ImageIcon("./img/mower.jpg");
		mower_icon = new ImageIcon(tmp.getImage().getScaledInstance(DEFAULT_PIXELS, DEFAULT_PIXELS, Image.SCALE_DEFAULT));
		tmp = new ImageIcon("./img/crash.jpg");
		crash_icon = new ImageIcon(tmp.getImage().getScaledInstance(DEFAULT_PIXELS, DEFAULT_PIXELS, Image.SCALE_DEFAULT));
		tmp = new ImageIcon("./img/charge.jpg");
		charge_icon = new ImageIcon(tmp.getImage().getScaledInstance(DEFAULT_PIXELS, DEFAULT_PIXELS, Image.SCALE_DEFAULT));
	}

	// CLASS DEFINITION
	class ImageRenderer extends DefaultTableCellRenderer {
		JLabel lbl1 = new JLabel();

		// this it the render class to put Jlabel in the table
		public Component getTableCellRendererComponent(JTable table, Object value1, boolean isSelected, boolean hasFocus, int row, int column) {
			lbl1.setIcon((ImageIcon) value1);
			return lbl1;
		}

	}

	//Code below this if for testing

	class ImageText{
		String label=new String();
		ImageIcon image=new ImageIcon();
		public String lbl() {
			return label;
		}
		public ImageIcon images(){
			return image;
		}
	}

	// For testing to add both images and text to the mower icon

	class LabelIconRenderer extends DefaultTableCellRenderer {

		public LabelIconRenderer() {
			setHorizontalTextPosition(JLabel.CENTER);
			setVerticalTextPosition(JLabel.TOP);
		}
		ImageIcon im=mower_icon;
		@Override
			public Component getTableCellRendererComponent(JTable table, Object
					value, boolean isSelected, boolean hasFocus, int row, int col) {
				JLabel r = (JLabel) super.getTableCellRendererComponent(
						table, value, isSelected, hasFocus, row, col);
				r.setIcon(im);
				r.setText((String)value);
				return r;


			}


	}
}
