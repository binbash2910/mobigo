import { IPeople } from 'app/entities/people/people.model';

export interface IVehicle {
  id: number;
  marque?: string | null;
  modele?: string | null;
  annee?: string | null;
  carteGrise?: string | null;
  immatriculation?: string | null;
  nbPlaces?: number | null;
  couleur?: string | null;
  photo?: string | null;
  actif?: string | null;
  proprietaire?: IPeople | null;
}

export type NewVehicle = Omit<IVehicle, 'id'> & { id: null };
