package autocompchem.datacollections;

/*
 *   Copyright (C) 2026  Marco Foscato
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 */

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.Locale;
import java.util.AbstractSet;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import autocompchem.io.ACCJson;
import autocompchem.io.IOtools;

/**
 * A {@link NamedDataCollector} that stores JSON-serializable {@link NamedData}
 * (see {@link NamedData#jsonable}) on disk under a dedicated directory instead
 * of retaining large values only on the Java heap space. Reference keys and
 * non-JSON-able entries (e.g. {@code FILE} for LOG/ERR paths) stay in memory.
 * <p>
 * Fetches by key load a single entry from disk; {@link #getAllNamedData()}
 * exposes a map view that loads values lazily per key access / iteration.
 * <p>
 * Spill file names are {@code <reference>.nd.json} when the reference is a
 * short, portable file stem; otherwise a URL-safe Base64 encoding is used
 * (same as older versions), which is still tried on read for migration.
 */
public class DiskSpillingNamedDataCollector extends NamedDataCollector
{
	private static final String SPILL_SUFFIX = ".nd.json";

	/** Max length of reference when used as the file stem (path portability). */
	private static final int MAX_PLAIN_REF_STEM_LENGTH = 200;

	private final File spillDirectory;
	private final Set<String> spilledKeys = new HashSet<String>();

//------------------------------------------------------------------------------

	public DiskSpillingNamedDataCollector(File spillDirectory) throws IOException
	{
		super();
		Objects.requireNonNull(spillDirectory, "spillDirectory");
		this.spillDirectory = spillDirectory.getAbsoluteFile();
		if (!this.spillDirectory.exists() && !this.spillDirectory.mkdirs())
		{
			throw new IOException("Cannot create spill directory: "
					+ this.spillDirectory);
		}
	}

//------------------------------------------------------------------------------

	/**
	 * @return the absolute directory where spilled JSON files are stored
	 */
	public File getSpillDirectory()
	{
		return spillDirectory;
	}

//------------------------------------------------------------------------------

	private static boolean shouldSpill(NamedData data)
	{
		return data != null && NamedData.jsonable.contains(data.getType());
	}

//------------------------------------------------------------------------------

	private static String base64StemForRef(String ref)
	{
		return Base64.getUrlEncoder().withoutPadding().encodeToString(
				ref.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @return true if {@code ref} is safe to use as a single path segment
	 * (no path separators, control chars, Windows reserved device names, etc.)
	 */
	static boolean canUsePlainReferenceFilename(String ref)
	{
		if (ref == null || ref.isEmpty() || ref.length() > MAX_PLAIN_REF_STEM_LENGTH)
		{
			return false;
		}
		if (ref.equals(".") || ref.equals("..") || ref.contains(".."))
		{
			return false;
		}
		for (int i = 0; i < ref.length(); i++)
		{
			char c = ref.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
					|| (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.')
			{
				continue;
			}
			return false;
		}
		String stem = ref.toUpperCase(Locale.ROOT);
		if (WINDOWS_RESERVED_STEMS.contains(stem))
		{
			return false;
		}
		if (stem.startsWith("COM") && stem.length() == 4
				&& stem.charAt(3) >= '1' && stem.charAt(3) <= '9')
		{
			return false;
		}
		if (stem.startsWith("LPT") && stem.length() == 4
				&& stem.charAt(3) >= '1' && stem.charAt(3) <= '9')
		{
			return false;
		}
		return true;
	}

	private static final Set<String> WINDOWS_RESERVED_STEMS = buildWindowsReserved();

	private static Set<String> buildWindowsReserved()
	{
		Set<String> s = new HashSet<String>();
		s.add("CON");
		s.add("PRN");
		s.add("AUX");
		s.add("NUL");
		return Collections.unmodifiableSet(s);
	}

	/**
	 * Primary on-disk file for this reference (plain or Base64 stem).
	 */
	private File spillFileForRef(String ref)
	{
		String basename = spillBasenameForRef(ref);
		return new File(spillDirectory, basename);
	}

	/**
	 * Legacy file from the Base64-only naming scheme.
	 */
	private File legacySpillFileForRef(String ref)
	{
		return new File(spillDirectory, base64StemForRef(ref) + SPILL_SUFFIX);
	}

	private static String spillBasenameForRef(String ref)
	{
		if (canUsePlainReferenceFilename(ref))
		{
			return ref + SPILL_SUFFIX;
		}
		return base64StemForRef(ref) + SPILL_SUFFIX;
	}

//------------------------------------------------------------------------------

	/**
	 * @param ref the reference of the {@link NamedData} to spill
	 * @param data the {@link NamedData} to spill
	 * @throws IOException if the spilled JSON file cannot be written
	 */
	private void writeSpilled(String ref, NamedData data) throws IOException
	{
		File primary = spillFileForRef(ref);
		try (FileWriter fw = new FileWriter(primary))
		{
			ACCJson.getWriter().toJson(data, fw);
		}
		File legacy = legacySpillFileForRef(ref);
		if (!primary.equals(legacy) && legacy.exists())
		{
			legacy.delete();
		}
	}

//------------------------------------------------------------------------------

	/**
	 * @param ref the reference of the {@link NamedData} to read
	 * @return the {@link NamedData} read from the spilled JSON file
	 * @throws IOException if the spilled JSON file cannot be read
	 */
	private NamedData readSpilled(String ref) throws IOException
	{
		File primary = spillFileForRef(ref);
		File toRead = primary;
		if (!toRead.exists())
		{
			File legacy = legacySpillFileForRef(ref);
			if (!primary.equals(legacy) && legacy.exists())
			{
				toRead = legacy;
			}
		}
		try (FileReader fr = new FileReader(toRead))
		{
			return ACCJson.getReader().fromJson(fr, NamedData.class);
		}
	}

//------------------------------------------------------------------------------

	/**
	 * @param ref the reference of the {@link NamedData} to delete
	 */
	private void deleteSpillFile(String ref)
	{
		File primary = spillFileForRef(ref);
		if (primary.exists())
		{
			primary.delete();
		}
		File legacy = legacySpillFileForRef(ref);
		if (!primary.equals(legacy) && legacy.exists())
		{
			legacy.delete();
		}
	}

//------------------------------------------------------------------------------

	@Override
	public void putNamedData(NamedData data)
	{
		String ref = data.getReference();
		if (shouldSpill(data))
		{
			allData.remove(ref);
			try
			{
				writeSpilled(ref, data);
				spilledKeys.add(ref);
			} catch (IOException e)
			{
				throw new RuntimeException(
						"Spilling NamedData to disk failed for key '" + ref
								+ "': " + e.getMessage(), e);
			}
		} else {
			if (spilledKeys.remove(ref))
			{
				deleteSpillFile(ref);
			}
			super.putNamedData(data);
		}
	}

//------------------------------------------------------------------------------

	@Override
	public void putNamedData(String ref, NamedData par)
	{
		par.setReference(ref);
		putNamedData(par);
	}

//------------------------------------------------------------------------------

	@Override
	public NamedData getNamedDataOrNull(String ref)
	{
		if (!contains(ref))
		{
			return null;
		}
		return getNamedData(ref, true);
	}

//------------------------------------------------------------------------------

	@Override
	public NamedData getNamedData(String ref, boolean tolerant)
	{
		if (spilledKeys.contains(ref))
		{
			try
			{
				return readSpilled(ref);
			} catch (IOException e)
			{
				if (tolerant)
				{
					return null;
				}
				throw new IllegalArgumentException("Key '" + ref
						+ "' is spilled but could not be read from disk: "
						+ e.getMessage(), e);
			}
		}
		return super.getNamedData(ref, tolerant);
	}

//------------------------------------------------------------------------------

	@Override
	public Map<String, NamedData> getAllNamedData()
	{
		return new AbstractMap<String, NamedData>()
		{
			@Override
			public int size()
			{
				return DiskSpillingNamedDataCollector.this.size();
			}

			@Override
			public boolean containsKey(Object key)
			{
				return key instanceof String
						&& DiskSpillingNamedDataCollector.this.contains(
								(String) key);
			}

			@Override
			public NamedData get(Object key)
			{
				if (!(key instanceof String))
				{
					return null;
				}
				String ks = (String) key;
				if (!contains(ks))
				{
					return null;
				}
				return getNamedData(ks, true);
			}

			@Override
			public Set<String> keySet()
			{
				Set<String> ks = new HashSet<String>(spilledKeys);
				ks.addAll(allData.keySet());
				return Collections.unmodifiableSet(ks);
			}

			@Override
			public Set<Entry<String, NamedData>> entrySet()
			{
				return new AbstractSet<Entry<String, NamedData>>()
				{
					@Override
					public int size()
					{
						return DiskSpillingNamedDataCollector.this.size();
					}

					@Override
					public Iterator<Entry<String, NamedData>> iterator()
					{
						final Iterator<String> ki = keySet().iterator();
						return new Iterator<Entry<String, NamedData>>()
						{
							@Override
							public boolean hasNext()
							{
								return ki.hasNext();
							}

							@Override
							public Entry<String, NamedData> next()
							{
								String k = ki.next();
								return new AbstractMap.SimpleImmutableEntry<String, NamedData>(
										k, getNamedData(k, false));
							}
						};
					}
				};
			}
		};
	}

//------------------------------------------------------------------------------

	@Override
	public boolean contains(String ref)
	{
		return spilledKeys.contains(ref) || super.contains(ref);
	}

//------------------------------------------------------------------------------

	@Override
	public void removeData(String ref)
	{
		if (spilledKeys.remove(ref))
		{
			deleteSpillFile(ref);
		}
		super.removeData(ref);
	}

//------------------------------------------------------------------------------

	@Override
	public void clear()
	{
		for (String k : spilledKeys)
		{
			deleteSpillFile(k);
		}
		spilledKeys.clear();
		super.clear();
	}

//------------------------------------------------------------------------------

	@Override
	public int size()
	{
		return spilledKeys.size() + allData.size();
	}

//------------------------------------------------------------------------------

	@Override
	public boolean isEmpty()
	{
		return spilledKeys.isEmpty() && super.isEmpty();
	}

//------------------------------------------------------------------------------

	@Override
	public NamedDataCollector copy()
	{
		NamedDataCollector ndc = new NamedDataCollector();
		for (String k : spilledKeys)
		{
			ndc.putNamedData(getNamedData(k));
		}
		for (NamedData e : allData.values())
		{
			ndc.putNamedData(e);
		}
		return ndc;
	}

//------------------------------------------------------------------------------

	@Override
	public NamedDataCollector clone() throws CloneNotSupportedException
	{
		NamedDataCollector ndc = new NamedDataCollector();
		for (String k : spilledKeys)
		{
			ndc.putNamedData(getNamedData(k).clone());
		}
		for (Map.Entry<String, NamedData> e : allData.entrySet())
		{
			ndc.putNamedData(e.getKey(), e.getValue().clone());
		}
		return ndc;
	}

//------------------------------------------------------------------------------

	@Override
	public boolean equals(Object o)
	{
		if (o == null)
		{
			return false;
		}
		if (o == this)
		{
			return true;
		}
		if (!(o instanceof NamedDataCollector))
		{
			return false;
		}
		NamedDataCollector other = (NamedDataCollector) o;
		if (other.size() != size())
		{
			return false;
		}
		for (String name : getAllNamedData().keySet())
		{
			NamedData oND = other.getNamedData(name, true);
			if (oND == null)
			{
				return false;
			}
			NamedData tND = getNamedData(name, true);
			if (!Objects.equals(tND, oND))
			{
				return false;
			}
		}
		return true;
	}

//------------------------------------------------------------------------------

	@Override
	public int hashCode()
	{
		return Objects.hash(spillDirectory, spilledKeys, allData);
	}

//------------------------------------------------------------------------------

	@Override
	public String toString()
	{
		return "[DiskSpillingNamedDataCollector spillDir=" + spillDirectory
				+ " spilledKeys=" + spilledKeys.size() + " memoryKeys="
				+ allData.size() + "]";
	}

//------------------------------------------------------------------------------
}
