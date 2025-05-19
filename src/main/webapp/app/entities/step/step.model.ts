import { IRide } from 'app/entities/ride/ride.model';

export interface IStep {
  id: number;
  ville?: string | null;
  heureDepart?: string | null;
  trajet?: IRide | null;
}

export type NewStep = Omit<IStep, 'id'> & { id: null };
