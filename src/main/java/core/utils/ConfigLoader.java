package core.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;

/**
 * Minimal ConfigLoader: layered .properties handling with concise debug/warn/error logging.
 */
public final class ConfigLoader {

    private ConfigLoader() {}

    // ================= Classpath Scope =================
    public enum ClasspathScope { TEST, MAIN, ANY }

    // ================== Basic Loads ====================
    public static Properties loadFromClasspath(String resourcePath) {
        return loadFromClasspath(resourcePath, ClasspathScope.ANY);
    }

    public static Properties loadFromClasspath(String resourcePath, ClasspathScope scope) {
        Properties p = new Properties();
        if (resourcePath == null || resourcePath.isBlank()) { core.logging.CustomLogger.warn.log("[cp] blank path"); return p; }
        long start = System.nanoTime();
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            core.logging.CustomLogger.debug.log("[cp:start] path=" + resourcePath + " scope=" + scope);
            if (scope == ClasspathScope.ANY) {
                try (InputStream in = cl.getResourceAsStream(resourcePath)) {
                    if (in != null) { p.load(in); core.logging.CustomLogger.debug.log("[cp:loaded] ANY keys=" + p.size()); }
                    else core.logging.CustomLogger.warn.log("[cp:miss] ANY path=" + resourcePath);
                }
                return p;
            }
            Enumeration<URL> urls = cl.getResources(resourcePath);
            List<URL> all = new ArrayList<>();
            while (urls.hasMoreElements()) all.add(urls.nextElement());
            URL chosen = null; String marker = "/test-classes/";
            if (!all.isEmpty()) {
                if (scope == ClasspathScope.TEST) for (URL u1 : all) if (urlLooksLikeTest(u1, marker)) { chosen = u1; break; }
                else if (scope == ClasspathScope.MAIN) for (URL u2 : all) if (!urlLooksLikeTest(u2, marker)) { chosen = u2; break; }
                if (chosen == null) chosen = all.get(0);
            }
            if (chosen != null) { try (InputStream in = chosen.openStream()) { p.load(in); } core.logging.CustomLogger.debug.log("[cp:loaded] scope=" + scope + " matches=" + all.size() + " keys=" + p.size()); }
            else core.logging.CustomLogger.warn.log("[cp:miss] scope=" + scope + " path=" + resourcePath);
        } catch (Exception e) {
            core.logging.CustomLogger.error.failed("[cp:error] path=" + resourcePath + " scope=" + scope + " msg=" + e.getMessage());
        } finally {
            core.logging.CustomLogger.debug.log("[cp:done] path=" + resourcePath + " ms=" + (System.nanoTime()-start)/1_000_000L);
        }
        return p;
    }

    private static boolean urlLooksLikeTest(URL u, String marker) {
        if (u == null) return false; String s = String.valueOf(u); return s.contains(marker) || s.contains("\\test-classes\\"); }

    public static Properties loadFromFile(Path path) {
        Properties p = new Properties(); long start = System.nanoTime();
        try {
            if (path != null && Files.exists(path)) { try (InputStream in = Files.newInputStream(path)) { p.load(in); } core.logging.CustomLogger.debug.log("[file:loaded] path=" + path + " keys=" + p.size()); }
            else core.logging.CustomLogger.warn.log("[file:miss] path=" + path);
        } catch (Exception e) {
            core.logging.CustomLogger.error.failed("[file:error] path=" + path + " msg=" + e.getMessage());
        } finally { core.logging.CustomLogger.debug.log("[file:done] path=" + path + " ms=" + (System.nanoTime()-start)/1_000_000L); }
        return p;
    }

    public static void writeToFile(Properties p, Path path, String comment) {
        Objects.requireNonNull(path, "path"); long start = System.nanoTime(); Properties src = (p==null?new Properties():p);
        try {
            if (path.getParent()!=null && !Files.exists(path.getParent())) { Files.createDirectories(path.getParent()); core.logging.CustomLogger.debug.log("[write:mkdir] parent=" + path.getParent()); }
            try(OutputStream out=Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) { src.store(out, comment==null?"":comment); }
            core.logging.CustomLogger.debug.log("[write:ok] path=" + path + " keys=" + src.size());
        } catch(Exception e){ core.logging.CustomLogger.error.failed("[write:error] path=" + path + " msg=" + e.getMessage()); throw new IllegalStateException("Failed write: " + path,e);}
        finally { core.logging.CustomLogger.debug.log("[write:done] path=" + path + " ms=" + (System.nanoTime()-start)/1_000_000L); }
    }

    // ================= Merge =================
    public static Properties merge(List<Properties> sources) {
        Properties out = new Properties(); int idx=0;
        for (Properties src : sources) { if (src==null){ core.logging.CustomLogger.debug.log("[merge:skip] idx="+idx); idx++; continue; }
            int before=out.size(); for(String k: src.stringPropertyNames()){ String v=src.getProperty(k); if(v!=null && !v.isBlank()) out.setProperty(k,v); }
            core.logging.CustomLogger.debug.log("[merge:layer] idx="+idx+" added="+(out.size()-before)+" total="+out.size()); idx++; }
        core.logging.CustomLogger.debug.log("[merge:done] layers="+sources.size()+" finalKeys="+out.size()); return out; }
    public static Properties merge(Properties... src){ return merge(Arrays.asList(src)); }

    // ================= Active =================
    private static final Properties ACTIVE = new Properties();
    public static synchronized void setActive(Properties p){ int prev=ACTIVE.size(); ACTIVE.clear(); if(p!=null) for(String k: p.stringPropertyNames()){ String v=p.getProperty(k); if(v!=null) ACTIVE.setProperty(k,v);} core.logging.CustomLogger.debug.log("[active:set] prev="+prev+" now="+ACTIVE.size()); }
    public static String get(String key){ String v=get(key,null); if(v==null){ core.logging.CustomLogger.error.failed("[get:miss] key="+key); throw new IllegalArgumentException("Missing config: "+key);} return v; }
    public static String get(String key, String def){ if(key==null||key.isBlank()){ core.logging.CustomLogger.warn.log("[get] blank key"); return def; }
        String envKey=key.toUpperCase(Locale.ROOT).replace('.', '_'); String src="DEFAULT"; String v=ACTIVE.getProperty(key);
        if(v!=null && !v.isBlank()) src="ACTIVE"; else { v=System.getProperty(key); if(v!=null && !v.isBlank()) src="SYSTEM"; else { v=System.getenv(envKey); if(v!=null && !v.isBlank()) src="ENV"; else v=def; } }
        core.logging.CustomLogger.debug.log("[get] key="+key+" src="+src+" val="+v); return v==null?def:v.trim(); }
    public static Properties cloneOf(Properties p){ Properties c=new Properties(); if(p!=null) for(String k: p.stringPropertyNames()){ String v=p.getProperty(k); if(v!=null) c.setProperty(k,v);} core.logging.CustomLogger.debug.log("[clone] size="+c.size()); return c; }

    // ================= Environment =================
    public static Properties environmentAsProperties(Map<String,String> explicit, BiFunction<String,String,String> mapper){ Properties out=new Properties(); Map<String,String> env=System.getenv(); int ex=0,map=0;
        if(explicit!=null) for(var e: explicit.entrySet()){ String val=env.get(e.getKey()); if(val!=null && !val.isBlank()){ out.setProperty(e.getValue(), val.trim()); ex++; } }
        if(mapper!=null) for(var e: env.entrySet()){ String tgt=mapper.apply(e.getKey(), e.getValue()); if(tgt!=null && !tgt.isBlank() && e.getValue()!=null && !e.getValue().isBlank()){ out.setProperty(tgt, e.getValue().trim()); map++; }}
        core.logging.CustomLogger.debug.log("[env] explicit="+ex+" mapped="+map+" total="+out.size()); return out; }

    // ================= Layered Builder =================
    public static final class Layered {
        private Layered() {}
        public static Builder builder(){ return new Builder(); }
        public static final class Builder {
            private static final class CpEntry { final String path; final ClasspathScope scope; CpEntry(String p, ClasspathScope s){ path=p; scope=s; } }
            private final List<CpEntry> cp=new ArrayList<>(); private final List<Path> files=new ArrayList<>(); private final List<Properties> extras=new ArrayList<>();
            private String sysKey="config.file"; private String envKey="CONFIG_FILE"; private boolean allowExt=true; private boolean incSys=true; private boolean incEnv=true; private final Map<String,String> envMap=new LinkedHashMap<>(); private BiFunction<String,String,String> envMapper;
            public Builder addClasspath(String r){ if(r!=null && !r.isBlank()) cp.add(new CpEntry(r, ClasspathScope.ANY)); return this; }
            public Builder addClasspath(String r, boolean test){ if(r!=null && !r.isBlank()) cp.add(new CpEntry(r, test?ClasspathScope.TEST:ClasspathScope.MAIN)); return this; }
            public Builder addClasspath(String r, ClasspathScope s){ if(r!=null && !r.isBlank()) cp.add(new CpEntry(r, s==null?ClasspathScope.ANY:s)); return this; }
            public Builder addFile(Path p){ if(p!=null) files.add(p); return this; }
            public Builder addProperties(Properties p){ if(p!=null && !p.isEmpty()) extras.add(p); return this; }
            public Builder externalOverrideKeys(String sys,String env){ sysKey=sys; envKey=env; return this; }
            public Builder allowExternalOverride(boolean b){ allowExt=b; return this; }
            public Builder includeSystemProperties(boolean b){ incSys=b; return this; }
            public Builder includeEnvironment(boolean b){ incEnv=b; return this; }
            public Builder mapEnv(String e,String k){ if(e!=null && k!=null && !k.isBlank()) envMap.put(e,k); return this; }
            public Builder mapEnv(Map<String,String> m){ if(m!=null) envMap.putAll(m); return this; }
            public Builder environmentMapper(BiFunction<String,String,String> m){ envMapper=m; return this; }
            public Properties build(){ List<Properties> layers=new ArrayList<>(); for(CpEntry c: cp) layers.add(loadFromClasspath(c.path,c.scope)); for(Path f: files) layers.add(loadFromFile(f)); layers.addAll(extras);
                if(allowExt){ Path ext=resolveExternal(sysKey, envKey); if(ext!=null) layers.add(loadFromFile(ext)); }
                if(incSys) layers.add(System.getProperties()); if(incEnv) layers.add(environmentAsProperties(envMap, envMapper)); return merge(layers); }
            private Path resolveExternal(String sp,String ek){ String viaSys= sp==null?null:System.getProperty(sp); String viaEnv= ek==null?null:System.getenv(ek); String first=firstNonBlank(viaSys, viaEnv); if(first==null||first.isBlank()) return null; Path p=Paths.get(first); if(!Files.exists(p)) core.logging.CustomLogger.warn.log("[ext:miss] path="+p); return p; }
        }
    }

    // ================= Temp =================
    public static Properties tempLoadOnly(Path p){ return loadFromFile(p); }
    public static Properties tempOverlay(Properties base, Path p){ Properties r=merge(base==null?new Properties():base, tempLoadOnly(p)); core.logging.CustomLogger.debug.log("[temp:overlay] base="+(base==null?0:base.size())+" over="+tempLoadOnly(p).size()+" result="+r.size()); return r; }
    public static String tempPull(Path p,String k){ String v=tempLoadOnly(p).getProperty(k); core.logging.CustomLogger.debug.log("[temp:pull] path="+p+" key="+k+" val="+v); return v; }
    public static Properties tempPullAll(Path p){ Properties all=tempLoadOnly(p); core.logging.CustomLogger.debug.log("[temp:all] path="+p+" keys="+all.size()); return all; }
    public static synchronized void tempPush(Path p,String k,String v){ Objects.requireNonNull(p); Objects.requireNonNull(k); Properties t=tempLoadOnly(p); if(v==null) t.remove(k); else t.setProperty(k,v); writeToFile(t,p,"temp"); core.logging.CustomLogger.debug.log("[temp:push] path="+p+" key="+k+" size="+t.size()); }
    public static synchronized void tempPushAll(Path p,Properties u,boolean overwrite){ Objects.requireNonNull(p); if(u==null) return; Properties w=overwrite?cloneOf(u):merge(tempLoadOnly(p),u); writeToFile(w,p,overwrite?"temp overwrite":"temp merge"); core.logging.CustomLogger.debug.log("[temp:pushAll] path="+p+" overwrite="+overwrite+" size="+w.size()); }
    public static synchronized void tempDeleteKey(Path p,String k){ Objects.requireNonNull(p); Properties t=tempLoadOnly(p); if(t.containsKey(k)){ t.remove(k); writeToFile(t,p,"temp del"); core.logging.CustomLogger.debug.log("[temp:del] path="+p+" key="+k); } else core.logging.CustomLogger.warn.log("[temp:del] missing key="+k+" path="+p); }
    public static synchronized void tempClear(Path p){ Objects.requireNonNull(p); writeToFile(new Properties(),p,"temp clear"); core.logging.CustomLogger.debug.log("[temp:clear] path="+p); }

    // ================= Template =================
    public static Path createTemplate(Path path, Collection<String> keys, Properties examples, boolean header, boolean overwrite){ Objects.requireNonNull(path); try { if(!overwrite && Files.exists(path)){ core.logging.CustomLogger.warn.log("[tpl:exists] path="+path); throw new IllegalStateException("Exists: "+path); }
            if(path.getParent()!=null) Files.createDirectories(path.getParent()); String nl=System.lineSeparator(); StringBuilder sb=new StringBuilder(256); if(header){ sb.append("# Template\n# Generated: ").append(new Date()).append('\n'); }
            int wrote=0; if(keys!=null) for(String k: keys){ if(k==null || k.isBlank()) continue; sb.append(k).append("=").append(nl); wrote++; if(examples!=null){ String ex=examples.getProperty(k); if(ex!=null && !ex.isBlank()) sb.append("# e.g. ").append(k).append("=").append(ex).append(nl); } }
            Files.writeString(path,sb.toString(),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING); core.logging.CustomLogger.debug.log("[tpl:ok] path="+path+" keys="+wrote); return path; }
        catch(Exception e){ core.logging.CustomLogger.error.failed("[tpl:error] path="+path+" msg="+e.getMessage()); throw new IllegalStateException("Template fail: "+path,e);} }

    // ================= Helpers =================
    private static String firstNonBlank(String... values){ if(values==null) return null; for(String v: values) if(v!=null && !v.isBlank()) return v; return null; }
}
