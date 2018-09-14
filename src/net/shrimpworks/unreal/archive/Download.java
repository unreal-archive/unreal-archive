package net.shrimpworks.unreal.archive;

import java.time.LocalDate;

public class Download {

	public String url;
	public LocalDate added;
	public LocalDate lastChecked;
	public boolean ok;                // health at last check date
	public boolean repack;

	public boolean deleted;

}
