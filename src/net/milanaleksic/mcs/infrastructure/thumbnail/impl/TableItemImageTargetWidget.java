package net.milanaleksic.mcs.infrastructure.thumbnail.impl;

import com.google.common.base.Optional;
import net.milanaleksic.mcs.domain.model.Film;
import net.milanaleksic.mcs.infrastructure.util.SWTUtil;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;

/**
 * User: Milan Aleksic
 * Date: 3/17/12
 * Time: 1:58 PM
 */
class TableItemImageTargetWidget implements ImageTargetWidget {

    private final TableItem tableItem;

    public TableItemImageTargetWidget(TableItem tableItem) {
        this.tableItem = tableItem;
    }

    public Optional<String> getImdbId() {
        return getFilm().isPresent() ? Optional.fromNullable(getFilm().get().getImdbId()) : Optional.<String>absent();
    }

    private Optional<Film> getFilm() {
        return Optional.fromNullable((Film) tableItem.getData());
    }

    @Override
    public void setImageFromExternalFile(String absoluteFileLocation) {
        tableItem.setImage(new Image(Display.getCurrent(), absoluteFileLocation));
    }

    @Override
    public void setImageFromResource(String imageResource) {
        SWTUtil.setImageOnTarget(tableItem, imageResource);
    }

    @Override
    public void safeSetImage(Optional<Image> image, String imdbId) {
        if (tableItem.isDisposed())
            return;
        Optional<String> targetImdbId = getImdbId();
        if (!targetImdbId.isPresent() || !targetImdbId.get().equals(imdbId))
            return;
        tableItem.setImage(image.orNull());
    }

}
