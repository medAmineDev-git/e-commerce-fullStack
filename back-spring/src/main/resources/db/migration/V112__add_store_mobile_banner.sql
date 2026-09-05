-- Une banniere par forme d'ecran.
--
-- Une seule image servait le mobile et le bureau. Recadree pour tenir dans un
-- bandeau large, elle perdait sur telephone ce qui en faisait le sujet : le
-- texte sortait du cadre, le vetement etait coupe. Le vendeur televerse
-- desormais deux visuels composes pour leur format respectif.
--
-- La colonne reste facultative : sans visuel mobile, la banniere de bureau
-- continue de servir, comme aujourd'hui.

ALTER TABLE stores ADD COLUMN banner_mobile_url VARCHAR(2000);
