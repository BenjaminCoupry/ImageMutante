import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

public class Tchernobyl {
    public static void main(String[] args) throws IOException {
        if(args.length == 0)
        {
            System.out.println("Tchernobyl <dossier source> <dossier fonds> <dossier destination> <nombre de mutations> <resolution des images finales>");
        }else {
            int mutations = Integer.parseInt(args[3]);
            int dimCible = Integer.parseInt(args[4]);
            traiterDossierdeDossiers(args[0],args[1], args[2], mutations, dimCible);
        }
    }
    public static Image openImage(String path)
    {
        try {
            Image picture = ImageIO.read(new File(path));
            return picture;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }
    public static void saveImage(String path,Image im)
    {
        try {
            ImageIO.write(toBufferedImage(im),"png",new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage cropImage(BufferedImage bufferedImage, int x, int y, int width, int height){
        BufferedImage croppedImage = bufferedImage.getSubimage(x, y, width, height);
        return croppedImage;
    }
    public static BufferedImage rotateImageByDegrees(BufferedImage img, double angle) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2, (newHeight - h) / 2);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return rotated;
    }
    public static BufferedImage resizeImage(BufferedImage bufferedImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(bufferedImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }
    public static BufferedImage rescaleImage(BufferedImage bufferedImage, double scale) throws IOException {
        double wx = bufferedImage.getWidth();
        double wy = bufferedImage.getHeight();
        return resizeImage(bufferedImage,(int)(wx*scale),(int)(wy*scale));
    }
    public static BufferedImage centrer(BufferedImage bufferedImage)
    {
        double wx = bufferedImage.getWidth();
        double wy = bufferedImage.getHeight();
        int l = (int)(Math.min(wx,wy));

        int x0 = 0;
        int y0 = 0;
        if(wx>wy)
        {
            y0=0;
            x0 = (int)((wx-l)/2);
        }else if(wy>wx)
        {
            x0=0;
            y0 = (int)((wy-l)/2);
        }
        bufferedImage = cropImage(bufferedImage,x0,y0,l,l);
        return bufferedImage;
    }
    public static BufferedImage copyImage(BufferedImage source){
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }
    public static BufferedImage muterTaille(BufferedImage bufferedImage,double RatioMin,Random r)throws IOException
    {
        double wx = bufferedImage.getWidth();
        double wy = bufferedImage.getHeight();
        int largMin = (int)(RatioMin* Math.min(wx,wy));
        if(largMin>Math.max(wx,wy))
        {
            largMin = (int)Math.max(wx,wy)-1;
        }
        int x0 = (int)((wx-largMin)*r.nextDouble());
        int y0 = (int)((wy-largMin)*r.nextDouble());
        int largMax = (int)Math.min(wx-x0,wy-y0)-1;
        int larg = (int)(Math.max(largMax*r.nextDouble(),largMin));
        bufferedImage = cropImage(bufferedImage,x0,y0,larg,larg);
        return bufferedImage;
    }
    public static BufferedImage muterTourner(BufferedImage bufferedImage,Random r)throws IOException
    {

        bufferedImage = centrer(bufferedImage);
        int l0 = (int)(bufferedImage.getWidth()/Math.sqrt(2));
        bufferedImage = rotateImageByDegrees(bufferedImage,r.nextDouble()*360.0);

        int wx = bufferedImage.getWidth();
        int wy = bufferedImage.getHeight();
        int l = (int)(Math.min(wx,wy));
        bufferedImage = cropImage(bufferedImage,l/2-l0/2,l/2-l0/2,(int)l0,(int)l0);
        return bufferedImage;
    }
    public static BufferedImage muterBruiter(BufferedImage bufferedImage,Random r)throws IOException
    {
        double sigma = r.nextDouble()*50;
        bufferedImage = copyImage(bufferedImage);
        int wx = bufferedImage.getWidth();
        int wy = bufferedImage.getHeight();
        for(int i=0;i<wx;i++)
        {
            for(int j=0;j<wy;j++)
            {
                Color c0 = new Color(bufferedImage.getRGB(i,j));
                int rc = (int)Math.max(0,Math.min(255,c0.getRed()+r.nextGaussian()*sigma));
                int gc = (int)Math.max(0,Math.min(255,c0.getGreen()+r.nextGaussian()*sigma));
                int bc = (int)Math.max(0,Math.min(255,c0.getBlue()+r.nextGaussian()*sigma));
                Color c = new Color(rc,gc,bc);
                bufferedImage.setRGB(i,j,c.getRGB());
            }
        }
        return bufferedImage;
    }
    public static void updateBord(int i,int j, int wx ,int wy,boolean[][] bords)
    {
        for(int u=-1;u<=1;u++)
        {
            for(int v=-1;v<=1;v++)
            {
                if(u!=0 || v != 0)
                {
                    int ir = Math.max(0,Math.min(wx-1,i+u));
                    int jr = Math.max(0,Math.min(wy-1,j+v));
                    boolean voisin = bords[ir][jr];
                    if(voisin)
                    {
                        bords[i][j] = true;
                    }
                }
            }
        }
    }
    public static boolean estBlanc(BufferedImage bufferedImage, int i, int j)
    {
        int mx = 200;
        Color c0 = new Color(bufferedImage.getRGB(i,j));
        boolean ret = c0.getRed()>mx && c0.getGreen()>mx && c0.getBlue()>mx;
        return ret;
    }
    public static boolean[][] calculerbords(BufferedImage bufferedImage)
    {
        int wx = bufferedImage.getWidth();
        int wy = bufferedImage.getHeight();
        boolean[][] bords = new boolean[wx][wy];
        for(int i=0;i<wx;i++)
        {
            for(int j=0;j<wy;j++)
            {
                if(i==0 || j==0 || i==wx-1 || j == wy-1) {
                    bords[i][j] = true;
                }
                else
                {
                    bords[i][j] = false;
                }
            }
        }

        for(int rx=0;rx<wx/2-1;rx++) {
            int ry=0;
            for (int i = rx; i < wx-rx; i++) {
                int j=ry;
                if(estBlanc(bufferedImage,i,j))
                {
                    updateBord(i,j,wx,wy,bords);
                }
                j=wy-ry-1;
                if(estBlanc(bufferedImage,i,j))
                {
                    updateBord(i,j,wx,wy,bords);
                }
            }
            for (int j = ry; j < wy-ry; j++) {
                int i=rx;
                if(estBlanc(bufferedImage,i,j))
                {
                    updateBord(i,j,wx,wy,bords);
                }
                i=wx-rx-1;
                if(estBlanc(bufferedImage,i,j))
                {
                    updateBord(i,j,wx,wy,bords);
                }
            }
        }

        for (int ry = 0; ry < wy/2-1; ry++) {
            int rx=0;
            for (int i = rx; i < wx-rx; i++) {
                int j=ry;
                if(estBlanc(bufferedImage,i,j))
                {
                    updateBord(i,j,wx,wy,bords);
                }
                j=wy-ry-1;
                if(estBlanc(bufferedImage,i,j))
                {
                    updateBord(i,j,wx,wy,bords);
                }
            }
            for (int j = ry; j < wy-ry; j++) {
                int i=rx;
                if(estBlanc(bufferedImage,i,j))
                {
                    updateBord(i,j,wx,wy,bords);
                }
                i=wx-rx-1;
                if(estBlanc(bufferedImage,i,j))
                {
                    updateBord(i,j,wx,wy,bords);
                }
            }
        }
        return bords;
    }
    public static BufferedImage antiFondBlanc(BufferedImage bufferedImage,BufferedImage nouveauFond,boolean[][] bords,Random r)throws IOException
    {
        bufferedImage = copyImage(bufferedImage);
        int wx = bufferedImage.getWidth();
        int wy = bufferedImage.getHeight();
        nouveauFond = resizeImage(nouveauFond,wx,wy);
        for(int i=0;i<wx;i++)
        {
            for(int j=0;j<wy;j++)
            {
                if(bords[i][j]) {
                    bufferedImage.setRGB(i, j, nouveauFond.getRGB(i,j));
                }
            }
        }
        return bufferedImage;
    }
    public static BufferedImage muterFlouter(BufferedImage bufferedImage,Random r)
    {
        int radius = (int)(r.nextDouble()*7)+1;
        int size = radius * 2 + 1;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];

        for (int i = 0; i < data.length; i++) {
            data[i] = weight;
        }

        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_ZERO_FILL, null);
        bufferedImage = op.filter(bufferedImage, null);
        int wx = bufferedImage.getWidth();
        int wy = bufferedImage.getHeight();
        int lx =wx-size;
        int ly = wy-size;
        bufferedImage = cropImage(bufferedImage,wx/2-lx/2,wy/2-ly/2,lx,ly);
        return bufferedImage;
    }
    public static BufferedImage muterTon(BufferedImage bufferedImage,Random r)
    {
        bufferedImage = copyImage(bufferedImage);
        int wx = bufferedImage.getWidth();
        int wy = bufferedImage.getHeight();
        boolean b = r.nextBoolean();
        int ton = (int)(r.nextDouble()*8)+1;
        for(int i=0;i<wx;i++)
        {
            for(int j=0;j<wy;j++)
            {
                Color c0 = new Color(bufferedImage.getRGB(i,j));
                Color c = c0;
                if(b) {
                    for(int k=0;k<ton;k++) {
                        c = c.brighter();
                    }
                }
                else
                {
                    for(int k=0;k<ton;k++) {
                        c = c.darker();
                    }
                }
                bufferedImage.setRGB(i,j,c.getRGB());
            }
        }
        return bufferedImage;
    }
    public static BufferedImage muter(BufferedImage bufferedImage, ArrayList<String> fonds, Random r, int dimCible, double p, boolean[][] bords) throws IOException {
        String fond = fonds.get(r.nextInt(fonds.size()));
        BufferedImage fnd = toBufferedImage(openImage(fond));
        bufferedImage = antiFondBlanc(bufferedImage,fnd,bords,r);
        if(r.nextDouble()<p) {
            bufferedImage = muterTaille(bufferedImage, 0.8, r);
        }
        if(r.nextDouble()<p) {
            bufferedImage = muterTourner(bufferedImage, r);
        }
        if(r.nextDouble()<p)
        {
            bufferedImage = muterTon(bufferedImage,r);
        }
        if(r.nextDouble()<p)
        {
            bufferedImage = muterFlouter(bufferedImage,r);
        }
        bufferedImage = resizeImage(centrer(bufferedImage),dimCible,dimCible);
        if(r.nextDouble()<p)
        {
            bufferedImage = muterBruiter(bufferedImage,r);
        }
        return bufferedImage;
    }
    public static void traiterDossier(String source,String fonds, String dest, int mutations,int dimCible) throws IOException {
        Random r = new Random();
        File directoryPath = new File(source);
        //List of all files and directories
        if( ! new File(dest).exists())
        {
            Files.createDirectories(Paths.get(dest));
        }
        File filesList[] = directoryPath.listFiles();
        ArrayList<String> fonds_ = (ArrayList<String>) Arrays.stream((new File(fonds)).listFiles()).map(f->f.toString()).collect(Collectors.toList());
        int ifil =0;
        for(File file : filesList) {
            ifil ++;
            String f = file.toString();
            System.out.println(f);
            try {
                BufferedImage bi = toBufferedImage(openImage(f));
                boolean[][] bords = calculerbords(bi);
                String nom = new File(source).getName()+"_"+ifil;//file.getName().replaceFirst("[.][^.]+$", "");
                saveImage(dest+"/"+nom+ "_init.bmp", resizeImage(centrer(bi),dimCible,dimCible));
                for (int i = 0; i < mutations; i++) {
                    try {

                        saveImage(dest+"/"+nom+"_mut" + i + ".bmp", muter(bi,fonds_, r, dimCible,0.2,bords));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void traiterDossierdeDossiers(String source,String fonds, String dest, int mutations,int dimCible) throws IOException {
        Random r = new Random();
        File directoryPath = new File(source);
        //List of all files and directories
        File filesList[] = directoryPath.listFiles();
        for(File file : filesList) {
            String f = file.toString();
            System.out.println(f);
            if(file.isDirectory())
            {
                String destination = dest +"/"+ file.getName();
                traiterDossier(f,fonds,destination,mutations,dimCible);
            }
        }
    }
}
